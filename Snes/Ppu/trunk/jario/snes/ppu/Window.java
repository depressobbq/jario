package jario.snes.ppu;

public class Window
{
	// Regs
	public boolean bg1_one_enable;
	public boolean bg1_one_invert;
	public boolean bg1_two_enable;
	public boolean bg1_two_invert;
	public boolean bg2_one_enable;
	public boolean bg2_one_invert;
	public boolean bg2_two_enable;
	public boolean bg2_two_invert;
	public boolean bg3_one_enable;
	public boolean bg3_one_invert;
	public boolean bg3_two_enable;
	public boolean bg3_two_invert;
	public boolean bg4_one_enable;
	public boolean bg4_one_invert;
	public boolean bg4_two_enable;
	public boolean bg4_two_invert;
	public boolean oam_one_enable;
	public boolean oam_one_invert;
	public boolean oam_two_enable;
	public boolean oam_two_invert;
	public boolean col_one_enable;
	public boolean col_one_invert;
	public boolean col_two_enable;
	public boolean col_two_invert;
	public int one_left;
	public int one_right;
	public int two_left;
	public int two_right;
	public int bg1_mask;
	public int bg2_mask;
	public int bg3_mask;
	public int bg4_mask;
	public int oam_mask;
	public int col_mask;
	public boolean bg1_main_enable;
	public boolean bg1_sub_enable;
	public boolean bg2_main_enable;
	public boolean bg2_sub_enable;
	public boolean bg3_main_enable;
	public boolean bg3_sub_enable;
	public boolean bg4_main_enable;
	public boolean bg4_sub_enable;
	public boolean oam_main_enable;
	public boolean oam_sub_enable;
	public int col_main_mask;
	public int col_sub_mask;

	// Output
	public boolean output_main_color_enable;
	public boolean output_sub_color_enable;

	private PPU self;
	private int x;
	private boolean one;
	private boolean two;
	private boolean main, sub;

	public final void scanline()
	{
		x = 0;
	}

	public final void run()
	{
		one = (x >= one_left && x <= one_right);
		two = (x >= two_left && x <= two_right);
		x++;

		test(
				bg1_one_enable, bg1_one_invert,
				bg1_two_enable, bg1_two_invert,
				bg1_mask, bg1_main_enable, bg1_sub_enable);
		if (main)
		{
			self.bg1.output.main.priority = 0;
		}
		if (sub)
		{
			self.bg1.output.sub.priority = 0;
		}

		test(
				bg2_one_enable, bg2_one_invert,
				bg2_two_enable, bg2_two_invert,
				bg2_mask, bg2_main_enable, bg2_sub_enable);
		if (main)
		{
			self.bg2.output.main.priority = 0;
		}
		if (sub)
		{
			self.bg2.output.sub.priority = 0;
		}

		test(
				bg3_one_enable, bg3_one_invert,
				bg3_two_enable, bg3_two_invert,
				bg3_mask, bg3_main_enable, bg3_sub_enable);
		if (main)
		{
			self.bg3.output.main.priority = 0;
		}
		if (sub)
		{
			self.bg3.output.sub.priority = 0;
		}

		test(
				bg4_one_enable, bg4_one_invert,
				bg4_two_enable, bg4_two_invert,
				bg4_mask, bg4_main_enable, bg4_sub_enable);
		if (main)
		{
			self.bg4.output.main.priority = 0;
		}
		if (sub)
		{
			self.bg4.output.sub.priority = 0;
		}

		test(
				oam_one_enable, oam_one_invert,
				oam_two_enable, oam_two_invert,
				oam_mask, oam_main_enable, oam_sub_enable);
		if (main)
		{
			self.sprite.output_main_priority = 0;
		}
		if (sub)
		{
			self.sprite.output_sub_priority = 0;
		}

		test(
				col_one_enable, col_one_invert,
				col_two_enable, col_two_invert,
				col_mask, true, true);

		switch (col_main_mask)
		{
		case 0:
			main = true;
			break;
		case 1:
			break;
		case 2:
			main = !main;
			break;
		case 3:
			main = false;
			break;
		}

		switch (col_sub_mask)
		{
		case 0:
			sub = true;
			break;
		case 1:
			break;
		case 2:
			sub = !sub;
			break;
		case 3:
			sub = false;
			break;
		}

		output_main_color_enable = main;
		output_sub_color_enable = sub;
	}

	public void reset()
	{
		bg1_one_enable = false;
		bg1_one_invert = false;
		bg1_two_enable = false;
		bg1_two_invert = false;
		bg2_one_enable = false;
		bg2_one_invert = false;
		bg2_two_enable = false;
		bg2_two_invert = false;
		bg3_one_enable = false;
		bg3_one_invert = false;
		bg3_two_enable = false;
		bg3_two_invert = false;
		bg4_one_enable = false;
		bg4_one_invert = false;
		bg4_two_enable = false;
		bg4_two_invert = false;
		oam_one_enable = false;
		oam_one_invert = false;
		oam_two_enable = false;
		oam_two_invert = false;
		col_one_enable = false;
		col_one_invert = false;
		col_two_enable = false;
		col_two_invert = false;
		one_left = 0;
		one_right = 0;
		two_left = 0;
		two_right = 0;
		bg1_mask = 0;
		bg2_mask = 0;
		bg3_mask = 0;
		bg4_mask = 0;
		oam_mask = 0;
		col_mask = 0;
		bg1_main_enable = false;
		bg1_sub_enable = false;
		bg2_main_enable = false;
		bg2_sub_enable = false;
		bg3_main_enable = false;
		bg3_sub_enable = false;
		bg4_main_enable = false;
		bg4_sub_enable = false;
		oam_main_enable = false;
		oam_sub_enable = false;
		col_main_mask = 0;
		col_sub_mask = 0;
		output_main_color_enable = false;
		output_sub_color_enable = false;

		x = 0;
		one = false;
		two = false;
	}

	public Window(PPU self)
	{
		this.self = self;
	}

	private final void test(boolean one_enable, boolean one_invert, boolean two_enable, boolean two_invert, int mask, boolean main_enable, boolean sub_enable)
	{
		boolean one = this.one ^ one_invert;
		boolean two = this.two ^ two_invert;
		boolean output = false;

		if (one_enable == false && two_enable == false)
		{
			output = false;
		}
		else if (one_enable == true && two_enable == false)
		{
			output = one;
		}
		else if (one_enable == false && two_enable == true)
		{
			output = two;
		}
		else
		{
			switch (mask)
			{
			case 0:
				output = (one | two) == true;
				break;
			case 1:
				output = (one & two) == true;
				break;
			case 2:
				output = (one ^ two) == true;
				break;
			case 3:
				output = (one ^ two) == false;
				break;
			}
		}

		main = main_enable ? output : false;
		sub = sub_enable ? output : false;
	}
}
