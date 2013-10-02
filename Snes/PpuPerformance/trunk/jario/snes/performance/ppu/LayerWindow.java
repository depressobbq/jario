/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.ppu;

import java.util.Arrays;

public class LayerWindow
{
	public boolean one_enable;
	public boolean one_invert;
	public boolean two_enable;
	public boolean two_invert;

	public int mask;

	public boolean main_enable;
	public boolean sub_enable;

	public int[] main = new int[256];
	public int[] sub = new int[256];

	public void render(boolean screen)
	{
		int[] output;
		if (screen == false)
		{
			output = main;
			if (main_enable == false)
			{
				Arrays.fill(output, 0);
				return;
			}
		}
		else
		{
			output = sub;
			if (sub_enable == false)
			{
				Arrays.fill(output, 0);
				return;
			}
		}

		if (one_enable == false && two_enable == false)
		{
			Arrays.fill(output, 0);
			return;
		}

		if (one_enable == true && two_enable == false)
		{
			boolean set = true ^ one_invert, clr = !set;
			for (int x = 0; x < 256; x++)
			{
				output[x] = (((x >= PPU.ppu.regs.window_one_left && x <= PPU.ppu.regs.window_one_right) ? set : clr) ? 1 : 0);
			}
			return;
		}

		if (one_enable == false && two_enable == true)
		{
			boolean set = true ^ two_invert, clr = !set;
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
				output[x] = ((one_mask | two_mask == true) ? 1 : 0);
				break;
			case 1:
				output[x] = ((one_mask & two_mask == true) ? 1 : 0);
				break;
			case 2:
				output[x] = ((one_mask ^ two_mask == true) ? 1 : 0);
				break;
			case 3:
				output[x] = ((one_mask ^ two_mask == false) ? 1 : 0);
				break;
			}
		}
	}
}
