package jario.snes.ppu;

import jario.snes.ppu.PPU.Output;

public class Background
{
	// Regs
	public int tiledata_addr;
	public int screen_addr;
	public int screen_size;
	public int mosaic;
	public boolean tile_size;
	public int mode;
	public int priority0;
	public int priority1;
	public boolean main_enable;
	public boolean sub_enable;
	public int hoffset;
	public int voffset;

	private PPU self;
	private int[] vram;

	public static final int ID_BG1 = 0;
	public static final int ID_BG2 = 1;
	public static final int ID_BG3 = 2;
	public static final int ID_BG4 = 3;
	public int id;

	public static final int Mode_BPP2 = 0;
	public static final int Mode_BPP4 = 1;
	public static final int Mode_BPP8 = 2;
	public static final int Mode_Mode7 = 3;
	public static final int Mode_Inactive = 4;

	public static final int ScreenSize_Size32x32 = 0;
	public static final int ScreenSize_Size32x64 = 1;
	public static final int ScreenSize_Size64x32 = 2;
	public static final int ScreenSize_Size64x64 = 3;

	public static final int TileSize_Size8x8 = 0;
	public static final int TileSize_Size16x16 = 1;

	public static final int Screen_Main = 0;
	public static final int Screen_Sub = 1;

	public Output output;

	public int x;
	public int y;

	public int mosaic_vcounter;
	public int mosaic_voffset;
	public int mosaic_hcounter;
	public int mosaic_hoffset;

	public int mosaic_priority;
	public int mosaic_palette;
	public int mosaic_tile;

	public int tile_counter;
	public int tile;
	public int priority;
	public int palette_number;
	public int palette_index;
	public int[] data = new int[8];

	public final void frame()
	{

	}

	public final void scanline()
	{
		boolean hires = (self.regs.bgmode == 5 || self.regs.bgmode == 6);
		x = -7;
		y = self.counter.vcounter();
		tile_counter = (7 - (hoffset & 7)) << (hires ? 1 : 0);
		for (int n = 0; n < 8; n++)
			data[n] = 0;

		if (self.counter.vcounter() == 1)
		{
			mosaic_vcounter = mosaic + 1;
			mosaic_voffset = 1;
		}
		else if (--mosaic_vcounter == 0)
		{
			mosaic_vcounter = mosaic + 1;
			mosaic_voffset += mosaic + 1;
		}

		mosaic_hcounter = mosaic + 1;
		mosaic_hoffset = 0;
	}

	public final void run(boolean screen)
	{
		boolean hires = (self.regs.bgmode == 5 || self.regs.bgmode == 6);

		if (screen) // Screen_Sub
		{
			output.main.priority = 0;
			output.sub.priority = 0;
			if (!hires) { return; }
		}

		if (mode == Mode_Inactive) { return; }
		if (!main_enable && !sub_enable) { return; }

		if (mode == Mode_Mode7)
		{
			run_mode7();
			return;
		}

		if (tile_counter-- == 0)
		{
			tile_counter = 7;
			get_tile();
		}

		int palette = get_tile_color() & 0xFF;
		if (x == 0)
		{
			mosaic_hcounter = 1;
		}
		if (x >= 0 && --mosaic_hcounter == 0)
		{
			mosaic_hcounter = mosaic + 1;
			mosaic_priority = priority;
			mosaic_palette = (palette != 0 ? palette_index + palette : 0);
			mosaic_tile = tile & 0xFFFF;
		}
		if (!screen) // Screen_Main
		{
			x++;
		}
		if (mosaic_palette == 0) { return; }

		if (!hires)
		{
			if (main_enable)
			{
				output.main.priority = mosaic_priority;
				output.main.palette = mosaic_palette;
				output.main.tile = mosaic_tile;
			}

			if (sub_enable)
			{
				output.sub.priority = mosaic_priority;
				output.sub.palette = mosaic_palette;
				output.sub.tile = mosaic_tile;
			}
		}
		else if (!screen) // Screen_Main
		{
			if (main_enable)
			{
				output.main.priority = mosaic_priority;
				output.main.palette = mosaic_palette;
				output.main.tile = mosaic_tile;
			}
		}
		else if (screen) // Screen_Sub
		{
			if (sub_enable)
			{
				output.sub.priority = mosaic_priority;
				output.sub.palette = mosaic_palette;
				output.sub.tile = mosaic_tile;
			}
		}
	}

	private final void get_tile()
	{
		boolean hires = (self.regs.bgmode == 5 || self.regs.bgmode == 6);

		int color_depth = mode == Mode_BPP2 ? 0 : mode == Mode_BPP4 ? 1 : 2;
		int palette_offset = (self.regs.bgmode == 0 ? (id << 5) : 0);
		int palette_size = 2 << color_depth;
		int tile_mask = 0x0fff >> color_depth;
		int tiledata_index = tiledata_addr >> (4 + color_depth);

		int tile_height = ((tile_size ? 1 : 0) == TileSize_Size8x8 ? 3 : 4);
		int tile_width = (!hires ? tile_height : 4);

		int width = 256 << (hires ? 1 : 0);

		int mask_x = (tile_height == 3 ? width : (width << 1));
		int mask_y = mask_x;
		if ((screen_size & 1) != 0)
		{
			mask_x <<= 1;
		}
		if ((screen_size & 2) != 0)
		{
			mask_y <<= 1;
		}
		mask_x--;
		mask_y--;

		int px = x << (hires ? 1 : 0);
		int py = (mosaic == 0 ? y : mosaic_voffset);

		int hscroll = hoffset;
		int vscroll = voffset;
		if (hires)
		{
			hscroll <<= 1;
			if (self.regs.interlace)
			{
				py = (py << 1) + (self.counter.field() ? 1 : 0);
			}
		}

		int hoffset = hscroll + px;
		int voffset = vscroll + py;

		if (self.regs.bgmode == 2 || self.regs.bgmode == 4 || self.regs.bgmode == 6)
		{
			int offset_x = (x + (hscroll & 7)) & 0xFFFF;

			if (offset_x >= 8)
			{
				int hval = self.bg3.get_tile((offset_x - 8) + (self.bg3.hoffset & ~7), self.bg3.voffset + 0);
				int vval = self.bg3.get_tile((offset_x - 8) + (self.bg3.hoffset & ~7), self.bg3.voffset + 8);
				int valid_mask = (id == ID_BG1 ? 0x2000 : 0x4000);

				if (self.regs.bgmode == 4)
				{
					if ((hval & valid_mask) != 0)
					{
						if ((hval & 0x8000) == 0)
						{
							hoffset = offset_x + (hval & ~7);
						}
						else
						{
							voffset = y + hval;
						}
					}
				}
				else
				{
					if ((hval & valid_mask) != 0)
					{
						hoffset = offset_x + (hval & ~7);
					}
					if ((vval & valid_mask) != 0)
					{
						voffset = y + vval;
					}
				}
			}
		}

		hoffset &= mask_x;
		voffset &= mask_y;

		int screen_x = ((screen_size & 1) != 0 ? 32 << 5 : 0);
		int screen_y = ((screen_size & 2) != 0 ? 32 << 5 : 0);
		if (screen_size == 3)
		{
			screen_y <<= 1;
		}

		int tx = hoffset >> tile_width;
		int ty = voffset >> tile_height;

		int offset = ((ty & 0x1f) << 5) + (tx & 0x1f);
		if ((tx & 0x20) != 0)
		{
			offset += screen_x;
		}
		if ((ty & 0x20) != 0)
		{
			offset += screen_y;
		}

		int addr = (screen_addr + (offset << 1)) & 0xFFFF;
		tile = vram[addr + 0] + (vram[addr + 1] << 8);
		boolean mirror_y = (tile & 0x8000) != 0;
		boolean mirror_x = (tile & 0x4000) != 0;
		priority = ((tile & 0x2000) != 0 ? priority1 : priority0);
		palette_number = (tile >> 10) & 7;
		palette_index = palette_offset + (palette_number << palette_size);

		if (tile_width == 4 && ((hoffset & 8) != 0) != mirror_x)
		{
			tile += 1;
		}
		if (tile_height == 4 && ((voffset & 8) != 0) != mirror_y)
		{
			tile += 16;
		}
		int character = ((tile & 0x03ff) + tiledata_index) & tile_mask;

		if (mirror_y)
		{
			voffset ^= 7;
		}
		offset = ((character << (4 + color_depth)) + ((voffset & 7) << 1)) & 0xFFFF;

		if (mode >= Mode_BPP2)
		{
			data[0] = vram[offset + 0];
			data[1] = vram[offset + 1];
		}
		if (mode >= Mode_BPP4)
		{
			data[2] = vram[offset + 16];
			data[3] = vram[offset + 17];
		}
		if (mode >= Mode_BPP8)
		{
			data[4] = vram[offset + 32];
			data[5] = vram[offset + 33];
			data[6] = vram[offset + 48];
			data[7] = vram[offset + 49];
		}

		if (mirror_x)
		{
			for (int n = 0; n < 8; n++)
			{
				// reverse data bits in data[n]: 01234567 -> 76543210
				data[n] = ((data[n] >> 4) & 0x0f) | ((data[n] << 4) & 0xf0);
				data[n] = ((data[n] >> 2) & 0x33) | ((data[n] << 2) & 0xcc);
				data[n] = ((data[n] >> 1) & 0x55) | ((data[n] << 1) & 0xaa);
			}
		}
	}

	private final int get_tile_color()
	{
		int color = 0;

		switch (mode)
		{
		case Mode_BPP8:
			color += (data[7] >> 0) & 0x80;
			data[7] <<= 1;
			color += (data[6] >> 1) & 0x40;
			data[6] <<= 1;
			color += (data[5] >> 2) & 0x20;
			data[5] <<= 1;
			color += (data[4] >> 3) & 0x10;
			data[4] <<= 1;
		case Mode_BPP4:
			color += (data[3] >> 4) & 0x08;
			data[3] <<= 1;
			color += (data[2] >> 5) & 0x04;
			data[2] <<= 1;
		case Mode_BPP2:
			color += (data[1] >> 6) & 0x02;
			data[1] <<= 1;
			color += (data[0] >> 7) & 0x01;
			data[0] <<= 1;
		}

		return color;
	}

	private final int get_tile(int x, int y)
	{
		boolean hires = (self.regs.bgmode == 5 || self.regs.bgmode == 6);
		int tile_height = ((tile_size ? 1 : 0) == TileSize_Size8x8 ? 3 : 4);
		int tile_width = (!hires ? tile_height : 4);
		int width = (!hires ? 256 : 512);
		int mask_x = (tile_height == 3 ? width : (width << 1));
		int mask_y = mask_x;
		if ((screen_size & 1) != 0)
		{
			mask_x <<= 1;
		}
		if ((screen_size & 2) != 0)
		{
			mask_y <<= 1;
		}
		mask_x--;
		mask_y--;

		int screen_x = ((screen_size & 1) != 0 ? (32 << 5) : 0);
		int screen_y = ((screen_size & 2) != 0 ? (32 << 5) : 0);
		if (screen_size == 3)
		{
			screen_y <<= 1;
		}

		x = (x & mask_x) >> tile_width;
		y = (y & mask_y) >> tile_height;

		int offset = ((y & 0x1f) << 5) + (x & 0x1f);
		if ((x & 0x20) != 0)
		{
			offset += screen_x;
		}
		if ((y & 0x20) != 0)
		{
			offset += screen_y;
		}

		int addr = (screen_addr + (offset << 1)) & 0xFFFF;
		return vram[addr + 0] + (vram[addr + 1] << 8);
	}

	public void reset()
	{
		tiledata_addr = 0;
		screen_addr = 0;
		screen_size = 0;
		mosaic = 0;
		tile_size = false;
		mode = 0;
		priority0 = 0;
		priority1 = 0;
		main_enable = false;
		sub_enable = false;
		hoffset = 0;
		voffset = 0;

		output.main.palette = 0;
		output.main.priority = 0;
		output.sub.palette = 0;
		output.sub.priority = 0;

		x = 0;
		y = 0;

		mosaic_vcounter = 0;
		mosaic_voffset = 0;
		mosaic_hcounter = 0;
		mosaic_hoffset = 0;

		mosaic_priority = 0;
		mosaic_palette = 0;
		mosaic_tile = 0;

		tile_counter = 0;
		tile = 0;
		priority = 0;
		palette_number = 0;
		palette_index = 0;
		for (int n = 0; n < 8; n++)
		{
			data[n] = 0;
		}
	}

	public Background(PPU self_, int id_)
	{
		self = self_;
		id = id_;
		output = new Output(id_);
		vram = self.vram;
	}

	private final int clip(int n)
	{ // 13-bit sign extend: --s---nnnnnnnnnn -> ssssssnnnnnnnnnn
		return (n & 0x2000) != 0 ? (n | ~1023) : (n & 1023);
	}

	private final void run_mode7()
	{
		int a = sclip(16, self.regs.m7a);
		int b = sclip(16, self.regs.m7b);
		int c = sclip(16, self.regs.m7c);
		int d = sclip(16, self.regs.m7d);

		int cx = sclip(13, self.regs.m7x);
		int cy = sclip(13, self.regs.m7y);
		int hoffset = sclip(13, self.regs.mode7_hoffset);
		int voffset = sclip(13, self.regs.mode7_voffset);

		if ((this.x++ & ~255) != 0) { return; }
		int x = mosaic_hoffset;
		int y = self.bg1.mosaic_voffset; // BG2 vertical mosaic uses BG1 mosaic size

		if (--mosaic_hcounter == 0)
		{
			mosaic_hcounter = mosaic + 1;
			mosaic_hoffset += mosaic + 1;
		}

		if (self.regs.mode7_hflip)
		{
			x = 255 - x;
		}
		if (self.regs.mode7_vflip)
		{
			y = 255 - y;
		}

		int psx = ((a * clip(hoffset - cx)) & ~63) + ((b * clip(voffset - cy)) & ~63) + ((b * y) & ~63) + (cx << 8);
		int psy = ((c * clip(hoffset - cx)) & ~63) + ((d * clip(voffset - cy)) & ~63) + ((d * y) & ~63) + (cy << 8);

		int px = psx + (a * x);
		int py = psy + (c * x);

		// mask pseudo-FP bits
		px >>= 8;
		py >>= 8;

		int tile;
		int palette = 0;
		switch (self.regs.mode7_repeat)
		{
		// screen repetition outside of screen area
		case 0:
		case 1:
		{
			px &= 1023;
			py &= 1023;
			tile = vram[((py >> 3) * 128 + (px >> 3)) << 1];
			palette = vram[(((tile << 6) + ((py & 7) << 3) + (px & 7)) << 1) + 1];
			break;
		}
		// palette color 0 outside of screen area
		case 2:
		{
			if (((px | py) & ~1023) != 0)
			{
				palette = 0;
			}
			else
			{
				px &= 1023;
				py &= 1023;
				tile = vram[((py >> 3) * 128 + (px >> 3)) << 1];
				palette = vram[(((tile << 6) + ((py & 7) << 3) + (px & 7)) << 1) + 1];
			}
			break;
		}
		// character 0 repetition outside of screen area
		case 3:
		{
			if (((px | py) & ~1023) != 0)
			{
				tile = 0;
			}
			else
			{
				px &= 1023;
				py &= 1023;
				tile = vram[((py >> 3) * 128 + (px >> 3)) << 1];
			}
			palette = vram[(((tile << 6) + ((py & 7) << 3) + (px & 7)) << 1) + 1];
			break;
		}
		}

		int priority = 0;
		if (id == ID_BG1)
		{
			priority = priority0;
		}
		else if (id == ID_BG2)
		{
			priority = ((palette & 0x80) != 0 ? priority1 : priority0);
			palette &= 0x7f;
		}

		if (palette == 0) { return; }

		if (main_enable)
		{
			output.main.palette = palette;
			output.main.priority = priority;
			output.main.tile = 0;
		}

		if (sub_enable)
		{
			output.sub.palette = palette;
			output.sub.priority = priority;
			output.main.tile = 0;
		}
	}

	private static final int sclip(int bits, int x)
	{
		int b = 1 << (bits - 1);
		int m = (1 << bits) - 1;
		return (((x & m) ^ b) - b);
	}
}
