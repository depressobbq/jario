package jario.snes.ppu;

import java.nio.ShortBuffer;

public class Screen
{
	// Regs
	public boolean addsub_mode;
	public boolean direct_color;
	public boolean color_mode;
	public boolean color_halve;
	public boolean bg1_color_enable;
	public boolean bg2_color_enable;
	public boolean bg3_color_enable;
	public boolean bg4_color_enable;
	public boolean oam_color_enable;
	public boolean back_color_enable;
	public int color_b;
	public int color_g;
	public int color_r;

	private PPU self;
	private int[] cgram;
	private ShortBuffer output_data;
	private int output_offset;

	public final void scanline()
	{
		output_data = self.output.asShortBuffer();
		output_offset = self.counter.status.vcounter * 1024;
		if (self.display.interlace && self.counter.status.field)
		{
			output_offset += 512;
		}
	}

	public final void run()
	{
		int color;
		if (!self.regs.pseudo_hires && self.regs.bgmode != 5 && self.regs.bgmode != 6)
		{
			color = get_pixel(false);
			output_data.put(output_offset, (short) color);
			output_data.put(output_offset + 1, (short) color);
		}
		else
		{
			color = get_pixel(true);
			output_data.put(output_offset, (short) color);
			color = get_pixel(false);
			output_data.put(output_offset + 1, (short) color);
		}

		output_offset += 2;
	}

	public void reset()
	{
		addsub_mode = false;
		direct_color = false;
		color_mode = false;
		color_halve = false;
		bg1_color_enable = false;
		bg2_color_enable = false;
		bg3_color_enable = false;
		bg4_color_enable = false;
		oam_color_enable = false;
		back_color_enable = false;
		color_r = 0;
		color_g = 0;
		color_b = 0;
	}

	public Screen(PPU self)
	{
		this.self = self;
		cgram = self.cgram;
		for (int l = 0; l < 16; l++)
		{
			for (int r = 0; r < 32; r++)
			{
				for (int g = 0; g < 32; g++)
				{
					for (int b = 0; b < 32; b++)
					{
						double luma = (double) l / 15.0;
						int ar = (int) (luma * r + 0.5);
						int ag = (int) (luma * g + 0.5);
						int ab = (int) (luma * b + 0.5);
						light_table[l][(r << 10) + (g << 5) + b] = (short) ((ab << 10) + (ag << 5) + ar);
					}
				}
			}
		}
	}

	private short[][] light_table = new short[16][32768];

	static final int Source_BG1 = 0;
	static final int Source_BG2 = 1;
	static final int Source_BG3 = 2;
	static final int Source_BG4 = 3;
	static final int Source_OAM = 4;
	static final int Source_BACK = 5;

	private final int get_pixel(boolean swap)
	{
		boolean[] color_enable = { bg1_color_enable, bg2_color_enable, bg3_color_enable, bg4_color_enable, oam_color_enable, back_color_enable };

		// ===========
		// main screen
		// ===========

		int priority_main = 0;
		int color_main = 0;
		int source_main = 0;

		if ((self.bg1.output.main.priority) != 0)
		{
			priority_main = self.bg1.output.main.priority;
			if (direct_color && (self.regs.bgmode == 3 || self.regs.bgmode == 4 || self.regs.bgmode == 7))
			{
				color_main = get_direct_color(self.bg1.output.main.palette, self.bg1.output.main.tile);
			}
			else
			{
				color_main = get_color(self.bg1.output.main.palette);
			}
			source_main = Source_BG1;
		}
		if (self.bg2.output.main.priority > priority_main)
		{
			priority_main = self.bg2.output.main.priority;
			color_main = get_color(self.bg2.output.main.palette);
			source_main = Source_BG2;
		}
		if (self.bg3.output.main.priority > priority_main)
		{
			priority_main = self.bg3.output.main.priority;
			color_main = get_color(self.bg3.output.main.palette);
			source_main = Source_BG3;
		}
		if (self.bg4.output.main.priority > priority_main)
		{
			priority_main = self.bg4.output.main.priority;
			color_main = get_color(self.bg4.output.main.palette);
			source_main = Source_BG4;
		}
		if (self.sprite.output_main_priority > priority_main)
		{
			priority_main = self.sprite.output_main_priority;
			color_main = get_color(self.sprite.output_main_palette);
			source_main = Source_OAM;
		}
		if (priority_main == 0)
		{
			color_main = get_color(0);
			source_main = Source_BACK;
		}

		// ==========
		// sub screen
		// ==========

		int priority_sub = 0;
		int color_sub = 0;
		int source_sub = 0;

		if (self.bg1.output.sub.priority != 0)
		{
			priority_sub = self.bg1.output.sub.priority;
			if (direct_color && (self.regs.bgmode == 3 || self.regs.bgmode == 4 || self.regs.bgmode == 7))
			{
				color_sub = get_direct_color(self.bg1.output.sub.palette, self.bg1.output.sub.tile);
			}
			else
			{
				color_sub = get_color(self.bg1.output.sub.palette);
			}
			source_sub = Source_BG1;
		}
		if (self.bg2.output.sub.priority > priority_sub)
		{
			priority_sub = self.bg2.output.sub.priority;
			color_sub = get_color(self.bg2.output.sub.palette);
			source_sub = Source_BG2;
		}
		if (self.bg3.output.sub.priority > priority_sub)
		{
			priority_sub = self.bg3.output.sub.priority;
			color_sub = get_color(self.bg3.output.sub.palette);
			source_sub = Source_BG3;
		}
		if (self.bg4.output.sub.priority > priority_sub)
		{
			priority_sub = self.bg4.output.sub.priority;
			color_sub = get_color(self.bg4.output.sub.palette);
			source_sub = Source_BG4;
		}
		if (self.sprite.output_sub_priority > priority_sub)
		{
			priority_sub = self.sprite.output_sub_priority;
			color_sub = get_color(self.sprite.output_sub_palette);
			source_sub = Source_OAM;
		}
		if (priority_sub == 0)
		{
			if (self.regs.pseudo_hires || self.regs.bgmode == 5 || self.regs.bgmode == 6)
			{
				color_sub = get_color(0);
			}
			else
			{
				color_sub = (color_b << 10) + (color_g << 5) + (color_r << 0);
			}
			source_sub = Source_BACK;
		}

		if (swap)
		{
			int temp;
			temp = priority_main;
			priority_main = priority_sub;
			priority_sub = temp;
			temp = color_main;
			color_main = color_sub;
			color_sub = temp;
			temp = source_main;
			source_main = source_sub;
			source_sub = temp;
		}

		int output;
		if (!addsub_mode)
		{
			source_sub = Source_BACK;
			color_sub = (color_b << 10) + (color_g << 5) + (color_r << 0);
		}

		if (!self.window.output_main_color_enable)
		{
			if (!self.window.output_sub_color_enable) { return 0x0000; }
			color_main = 0x0000;
		}

		boolean color_exempt = (source_main == Source_OAM && self.sprite.output_main_palette < 192);
		if (!color_exempt && color_enable[source_main] && self.window.output_sub_color_enable)
		{
			boolean halve = false;
			if (color_halve && self.window.output_main_color_enable)
			{
				if (!addsub_mode || source_sub != Source_BACK)
				{
					halve = true;
				}
			}
			output = addsub(color_main & 0xFFFF, color_sub & 0xFFFF, halve);
		}
		else
		{
			output = color_main;
		}

		// ========
		// lighting
		// ========

		output = light_table[self.regs.display_brightness][output & 0xFFFF];
		if (self.regs.display_disable)
		{
			output = 0x0000;
		}
		return output;
	}

	private final int addsub(int x, int y, boolean halve)
	{
		if (!color_mode)
		{
			if (!halve)
			{
				int sum = x + y;
				int carry = (sum - ((x ^ y) & 0x0421)) & 0x8420;
				return ((sum - carry) | (carry - (carry >> 5)));
			}
			else
			{
				return ((x + y - ((x ^ y) & 0x0421)) >> 1);
			}
		}
		else
		{
			int diff = x - y + 0x8420;
			int borrow = (diff - ((x ^ y) & 0x8420)) & 0x8420;
			if (!halve)
			{
				return ((diff - borrow) & (borrow - (borrow >> 5)));
			}
			else
			{
				return ((((diff - borrow) & (borrow - (borrow >> 5))) & 0x7bde) >> 1);
			}
		}
	}

	private final int get_color(int palette)
	{
		palette <<= 1;
		self.regs.cgram_iaddr = palette & 0x1FF;
		return (cgram[palette + 0] + (cgram[palette + 1] << 8));
	}

	private final int get_direct_color(int palette, int tile)
	{ // palette = -------- BBGGGRRR
		// tile = ---bgr-- --------
		// output = 0BBb00GG Gg0RRRr0
		return (((palette << 7) & 0x6000) + ((tile >> 0) & 0x1000)
				+ ((palette << 4) & 0x0380) + ((tile >> 5) & 0x0040)
				+ ((palette << 2) & 0x001c) + ((tile >> 9) & 0x0002));
	}
}
