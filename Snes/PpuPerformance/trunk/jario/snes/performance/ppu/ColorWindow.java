/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.ppu;

import java.util.Arrays;

public class ColorWindow
{
	public boolean one_enable;
	public boolean one_invert;
	public boolean two_enable;
	public boolean two_invert;

	public int mask;

	public int main_mask;
	public int sub_mask;

	public int[] main = new int[256];
	public int[] sub = new int[256];

	public void render(boolean screen)
	{
		int[] output = (screen == false ? main : sub);
		boolean set = true, clr = false;

		switch (screen == false ? main_mask : sub_mask)
		{
		case 0:
			Arrays.fill(output, 1);
			return; // always
		case 1:
			set = true;
			clr = false;
			break; // inside window only
		case 2:
			set = false;
			clr = true;
			break; // outside window only
		case 3:
			Arrays.fill(output, 0);
			return; // never
		}

		if (one_enable == false && two_enable == false)
		{
			Arrays.fill(output, (clr ? 1 : 0));
			return;
		}

		if (one_enable == true && two_enable == false)
		{
			if (one_invert)
			{
				set ^= true;
				clr ^= true;
			}
			for (int x = 0; x < 256; x++)
			{
				output[x] = (((x >= PPU.ppu.regs.window_one_left && x <= PPU.ppu.regs.window_one_right) ? set : clr) ? 1 : 0);
			}
			return;
		}

		if (one_enable == false && two_enable == true)
		{
			if (two_invert)
			{
				set ^= true;
				clr ^= true;
			}
			for (int x = 0; x < 256; x++)
			{
				output[x] = (((x >= PPU.ppu.regs.window_two_left && x <= PPU.ppu.regs.window_two_right) ? set : clr) ? 1 : 0);
			}
			return;
		}

		for (int x = 0; x < 256; x++)
		{
			boolean one_mask = (x >= PPU.ppu.regs.window_one_left && x <= PPU.ppu.regs.window_one_right) ^ one_invert;
			boolean two_mask = (x >= PPU.ppu.regs.window_two_left && x <= PPU.ppu.regs.window_two_right) ^ two_invert;
			switch (mask)
			{
			case 0:
				output[x] = ((one_mask | two_mask == true ? set : clr) ? 1 : 0);
				break;
			case 1:
				output[x] = ((one_mask & two_mask == true ? set : clr) ? 1 : 0);
				break;
			case 2:
				output[x] = ((one_mask ^ two_mask == true ? set : clr) ? 1 : 0);
				break;
			case 3:
				output[x] = ((one_mask ^ two_mask == false ? set : clr) ? 1 : 0);
				break;
			}
		}
	}
}
