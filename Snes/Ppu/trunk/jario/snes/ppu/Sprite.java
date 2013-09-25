package jario.snes.ppu;

import java.util.Arrays;

public class Sprite
{
	// Regs
	public boolean main_enable;
	public boolean sub_enable;
	public boolean interlace;
	public int base_size;
	public int nameselect;
	public int tiledata_addr;
	public int first_sprite;
	public int priority0;
	public int priority1;
	public int priority2;
	public int priority3;
	public boolean time_over;
	public boolean range_over;

	// Output
	public int output_main_priority;
	public int output_main_palette;
	public int output_sub_priority;
	public int output_sub_palette;

	private PPU self;
	private int[] oamram;
	private int[] vram;
	private SpriteItem[] list = new SpriteItem[128];
	private State t = new State();

	public final void update(int addr, int data)
	{
		oamram[addr] = data & 0xFF;

		if (addr < 0x0200)
		{
			int n = addr >> 2;
			addr &= 3;
			if (addr == 0)
			{
				list[n].x = (list[n].x & 0x100) | (data & 0xFF);
			}
			else if (addr == 1)
			{
				list[n].y = data & 0xFF;
			}
			else if (addr == 2)
			{
				list[n].character = data & 0xFF;
			}
			else
			{
				list[n].vflip = (data & 0x80) != 0;
				list[n].hflip = (data & 0x40) != 0;
				list[n].priority = (data >> 4) & 3;
				list[n].palette = (data >> 1) & 7;
				list[n].nameselect = (data & 1) != 0;
			}
		}
		else
		{
			int n = (addr & 0x1f) << 2;
			list[n + 0].x = ((data & 0x01) << 8) | (list[n + 0].x & 0xff);
			list[n + 0].size = (data & 0x02) != 0;
			list[n + 1].x = ((data & 0x04) << 6) | (list[n + 1].x & 0xff);
			list[n + 1].size = (data & 0x08) != 0;
			list[n + 2].x = ((data & 0x10) << 4) | (list[n + 2].x & 0xff);
			list[n + 2].size = (data & 0x20) != 0;
			list[n + 3].x = ((data & 0x40) << 2) | (list[n + 3].x & 0xff);
			list[n + 3].size = (data & 0x80) != 0;
		}
	}

	public final void address_reset()
	{
		self.regs.oam_addr = self.regs.oam_baseaddr & 0x3FF;
		set_first_sprite();
	}

	public final void set_first_sprite()
	{
		first_sprite = (self.regs.oam_priority == false ? 0 : (self.regs.oam_addr >> 2) & 127);
	}

	public final void frame()
	{
		time_over = false;
		range_over = false;
	}

	public final void scanline()
	{
		t.x = 0;
		t.y = self.counter.status.vcounter;

		t.item_count = 0;
		t.tile_count = 0;

		t.active = !t.active;
		int[] oam_item = t.item[t.active ? 1 : 0];
		TileItem[] oam_tile = t.tile[t.active ? 1 : 0];

		if (t.y == (!self.regs.overscan ? 225 : 240) && self.regs.display_disable == false)
		{
			address_reset();
		}
		if (t.y >= (!self.regs.overscan ? 224 : 239)) { return; }

		Arrays.fill(oam_item, 0, 32, 0xff); // default to invalid
		for (int i = 0; i < 34; i++)
		{
			oam_tile[i].x = 0xffff; // default to invalid
		}

		for (int i = 0; i < 128; i++)
		{
			int sprite = (first_sprite + i) & 127;
			if (on_scanline(list[sprite]) == false)
			{
				continue;
			}
			if (t.item_count++ >= 32)
			{
				break;
			}
			oam_item[t.item_count - 1] = sprite;
		}

		// TODO: Remove this hack
		if (t.item_count > 0 && (t.item_count > oam_item.length))
		{
			self.regs.oam_iaddr = (0x0200 + (0 >> 2)) & 0x3FF;
		}
		else if (t.item_count > 0 && oam_item[t.item_count - 1] != 0xff)
		{
			self.regs.oam_iaddr = (0x0200 + (oam_item[t.item_count - 1] >> 2)) & 0x3FF;
		}
	}

	public final void run()
	{
		output_main_priority = 0;
		output_sub_priority = 0;

		TileItem[] oam_tile = t.tile[!t.active ? 1 : 0];
		int[] priority_table = { priority0, priority1, priority2, priority3 };
		int x = t.x++;

		for (int n = 0; n < 34; n++)
		{
			TileItem tile = oam_tile[n];
			if (tile.x == 0xffff)
			{
				break;
			}

			int px = x - sclip(9, tile.x);
			if ((px & ~7) != 0)
			{
				continue;
			}

			int mask = 0x80 >> (tile.hflip == false ? px : 7 - px);
			int color;
			color = (((tile.d0 & mask) != 0) ? 1 : 0) << 0;
			color |= (((tile.d1 & mask) != 0) ? 1 : 0) << 1;
			color |= (((tile.d2 & mask) != 0) ? 1 : 0) << 2;
			color |= (((tile.d3 & mask) != 0) ? 1 : 0) << 3;

			if (color != 0)
			{
				if (main_enable)
				{
					output_main_palette = (tile.palette + color) & 0xFF;
					output_main_priority = priority_table[tile.priority];
				}

				if (sub_enable)
				{
					output_sub_palette = (tile.palette + color) & 0xFF;
					output_sub_priority = priority_table[tile.priority];
				}
			}
		}
	}

	int i = 31;
	int tx = 0;

	public final boolean tilefetch_cycle()
	{
		int[] oam_item = t.item[t.active ? 1 : 0];
		TileItem[] oam_tile = t.tile[t.active ? 1 : 0];

		while (i >= 0)
		{
			if (oam_item[i] == 0xff)
			{
				i--;
				continue;
			}
			SpriteItem sprite = list[oam_item[i]];

			int tile_width = sprite.width(base_size) >> 3;
			int x = sprite.x;
			int y = (t.y - sprite.y) & 0xff;
			if (interlace)
			{
				y <<= 1;
			}

			if (sprite.vflip)
			{
				if (sprite.width(base_size) == sprite.height(base_size, interlace))
				{
					y = (sprite.height(base_size, interlace) - 1) - y;
				}
				else if (y < sprite.width(base_size))
				{
					y = (sprite.width(base_size) - 1) - y;
				}
				else
				{
					y = sprite.width(base_size) + ((sprite.width(base_size) - 1) - (y - sprite.width(base_size)));
				}
			}

			if (interlace)
			{
				y = (sprite.vflip == false ? y + (self.counter.status.field ? 1 : 0) : y - (self.counter.status.field ? 1 : 0));
			}

			x &= 511;
			y &= 255;

			int tiledata_addr_ = tiledata_addr;
			int chrx = (sprite.character >> 0) & 15;
			int chry = (sprite.character >> 4) & 15;
			if (sprite.nameselect)
			{
				tiledata_addr_ += (256 * 32) + (nameselect << 13);
			}
			chry += (y >> 3);
			chry &= 15;
			chry <<= 4;

			while (tx < tile_width)
			{
				int sx = (x + (tx << 3)) & 511;
				if (x != 256 && sx >= 256 && (sx + 7) < 512)
				{
					tx++;
					continue;
				}
				if (t.tile_count++ >= 34)
				{
					break;
				}

				int n = t.tile_count - 1;
				oam_tile[n].x = sx;
				oam_tile[n].priority = sprite.priority;
				oam_tile[n].palette = 128 + (sprite.palette << 4);
				oam_tile[n].hflip = sprite.hflip;

				int mx = (sprite.hflip == false) ? tx : ((tile_width - 1) - tx);
				int pos = tiledata_addr_ + ((chry + ((chrx + mx) & 15)) << 5);
				int addr = (pos & 0xffe0) + ((y & 7) * 2);

				oam_tile[n].d0 = vram[addr + 0];
				oam_tile[n].d1 = vram[addr + 1];
				oam_tile[n].d2 = vram[addr + 16];
				oam_tile[n].d3 = vram[addr + 17];
				// self.add_clocks(4);
				self.ticks = (4 >> 1);
				tx++;
				if (tx >= tile_width)
				{
					i--;
					tx = 0;
				}
				return false;
			}
			i--;
			tx = 0;
		}
		i = 31;

		if (t.tile_count < 34)
		{
			// self.add_clocks((34 - t.tile_count) * 4);
			self.ticks = (((34 - t.tile_count) * 4) >> 1);
		}
		time_over |= (t.tile_count > 34);
		range_over |= (t.item_count > 32);

		return true;
	}

	final void tilefetch()
	{
		int[] oam_item = t.item[t.active ? 1 : 0];
		TileItem[] oam_tile = t.tile[t.active ? 1 : 0];

		for (int i = 31; i >= 0; i--)
		{
			if (oam_item[i] == 0xff)
			{
				continue;
			}
			SpriteItem sprite = list[oam_item[i]];

			int tile_width = sprite.width(base_size) >> 3;
			int x = sprite.x;
			int y = (t.y - sprite.y) & 0xff;
			if (interlace)
			{
				y <<= 1;
			}

			if (sprite.vflip)
			{
				if (sprite.width(base_size) == sprite.height(base_size, interlace))
				{
					y = (sprite.height(base_size, interlace) - 1) - y;
				}
				else if (y < sprite.width(base_size))
				{
					y = (sprite.width(base_size) - 1) - y;
				}
				else
				{
					y = sprite.width(base_size) + ((sprite.width(base_size) - 1) - (y - sprite.width(base_size)));
				}
			}

			if (interlace)
			{
				y = (sprite.vflip == false ? y + (self.counter.status.field ? 1 : 0) : y - (self.counter.status.field ? 1 : 0));
			}

			x &= 511;
			y &= 255;

			int tiledata_addr_ = tiledata_addr;
			int chrx = (sprite.character >> 0) & 15;
			int chry = (sprite.character >> 4) & 15;
			if (sprite.nameselect)
			{
				tiledata_addr_ += (256 * 32) + (nameselect << 13);
			}
			chry += (y >> 3);
			chry &= 15;
			chry <<= 4;

			for (int tx = 0; tx < tile_width; tx++)
			{
				int sx = (x + (tx << 3)) & 511;
				if (x != 256 && sx >= 256 && (sx + 7) < 512)
				{
					continue;
				}
				if (t.tile_count++ >= 34)
				{
					break;
				}

				int n = t.tile_count - 1;
				oam_tile[n].x = sx;
				oam_tile[n].priority = sprite.priority;
				oam_tile[n].palette = 128 + (sprite.palette << 4);
				oam_tile[n].hflip = sprite.hflip;

				int mx = (sprite.hflip == false) ? tx : ((tile_width - 1) - tx);
				int pos = tiledata_addr_ + ((chry + ((chrx + mx) & 15)) << 5);
				int addr = (pos & 0xffe0) + ((y & 7) * 2);

				oam_tile[n].d0 = vram[addr];
				oam_tile[n].d1 = vram[addr + 1];
				self.add_clocks(2);

				oam_tile[n].d2 = vram[addr + 16];
				oam_tile[n].d3 = vram[addr + 17];
				self.add_clocks(2);
			}
		}

		if (t.tile_count < 34)
		{
			self.add_clocks((34 - t.tile_count) * 4);
		}
		time_over |= (t.tile_count > 34);
		range_over |= (t.item_count > 32);
	}

	public void reset()
	{
		for (int i = 0; i < 128; i++)
		{
			list[i].x = 0;
			list[i].y = 0;
			list[i].character = 0;
			list[i].nameselect = false;
			list[i].vflip = false;
			list[i].hflip = false;
			list[i].priority = 0;
			list[i].palette = 0;
			list[i].size = false;
		}

		t.x = 0;
		t.y = 0;

		t.item_count = 0;
		t.tile_count = 0;

		t.active = false;
		for (int n = 0; n < 2; n++)
		{
			Arrays.fill(t.item[n], 0);
			for (int i = 0; i < 34; i++)
			{
				t.tile[n][i].x = 0;
				t.tile[n][i].priority = 0;
				t.tile[n][i].palette = 0;
				t.tile[n][i].hflip = false;
				t.tile[n][i].d0 = 0;
				t.tile[n][i].d1 = 0;
				t.tile[n][i].d2 = 0;
				t.tile[n][i].d3 = 0;
			}
		}

		main_enable = false;
		sub_enable = false;
		interlace = false;

		base_size = 0;
		nameselect = 0;
		tiledata_addr = 0;
		first_sprite = 0;

		priority0 = 0;
		priority1 = 0;
		priority2 = 0;
		priority3 = 0;

		time_over = false;
		range_over = false;

		output_main_palette = 0;
		output_main_priority = 0;
		output_sub_palette = 0;
		output_sub_priority = 0;
	}

	public Sprite(PPU self_)
	{
		self = self_;
		oamram = self.oamram;
		vram = self.vram;

		for (int i = 0; i < list.length; i++)
		{
			list[i] = new SpriteItem();
		}
	}

	private final boolean on_scanline(SpriteItem sprite)
	{
		if (sprite.x > 256 && (sprite.x + sprite.width(base_size) - 1) < 512) { return false; }
		int height = (interlace == false ? sprite.height(base_size, interlace) : (sprite.height(base_size, interlace) >> 1));
		if (t.y >= sprite.y && t.y < (sprite.y + height)) { return true; }
		if ((sprite.y + height) >= 256 && t.y < ((sprite.y + height) & 255)) { return true; }
		return false;
	}

	private static final int sclip(int bits, int x)
	{
		int b = 1 << (bits - 1);
		int m = (1 << bits) - 1;
		return ((x & m) ^ b) - b;
	}
}
