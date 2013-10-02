/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.cpu;

import jario.hardware.Bus1bit;

public class HVCounter
{
	class Status
	{
		public boolean interlace;
		public boolean field;
		public int vcounter;
		public int hcounter;
	}
	
	public class History
	{
		public boolean[] field = new boolean[2048];
		public int[] vcounter = new int[2048];
		public int[] hcounter = new int[2048];

		public int index;
	}
	
	Bus1bit ppu1bit;
	private Status status = new Status();
	private History history = new History();
	Runnable scanline;
	int region;
	
	public HVCounter()
	{
	}

	final void tick()
	{
		status.hcounter += 2; // increment by smallest unit of time
		if (status.hcounter >= 1360 && status.hcounter == lineclocks())
		{
			status.hcounter = 0;
			vcounter_tick();
		}

		history.index = (history.index + 1) & 2047;
		history.field[history.index] = status.field;
		history.vcounter[history.index] = status.vcounter;
		history.hcounter[history.index] = status.hcounter;
	}
	
	final boolean field()
	{
		return status.field;
	}

	final int vcounter()
	{
		return status.vcounter;
	}

	final int hcounter()
	{
		return status.hcounter;
	}
	
	final boolean field(int offset)
	{
		return history.field[(history.index - (offset >> 1)) & 2047];
	}
	
	final int vcounter(int offset)
	{
		return history.vcounter[(history.index - (offset >> 1)) & 2047];
	}
	
	final int hcounter(int offset)
	{
		return history.hcounter[(history.index - (offset >> 1)) & 2047];
	}

	final int hdot()
	{
		if (region == CPU.NTSC && !status.interlace && status.vcounter == 240 && status.field)
		{
			return (status.hcounter >> 2);
		}
		else
		{
			return ((status.hcounter - (((status.hcounter > 1292) ? 1 : 0) << 1) - (((status.hcounter > 1310) ? 1 : 0) << 1)) >> 2);
		}
	}

	final int lineclocks()
	{
		if (region == CPU.NTSC && !status.interlace && status.vcounter == 240 && status.field) { return 1360; }
		return 1364;
	}

	void reset()
	{
		status.interlace = false;
		status.field = false;
		status.vcounter = 0;
		status.hcounter = 0;
		
		history.index = 0;
		for (int i = 0; i < 2048; i++)
		{
			history.field[i] = false;
			history.vcounter[i] = 0;
			history.hcounter[i] = 0;
		}
	}

	private void vcounter_tick()
	{
		if (++status.vcounter == 128)
		{
			status.interlace = ppu1bit.read1bit(0);
		}

		if ((region == CPU.NTSC && status.interlace == false && status.vcounter == 262)
				|| (region == CPU.NTSC && status.interlace == true && status.vcounter == 263)
				|| (region == CPU.NTSC && status.interlace == true && status.vcounter == 262 && status.field == true)
				|| (region == CPU.PAL && status.interlace == false && status.vcounter == 312)
				|| (region == CPU.PAL && status.interlace == true && status.vcounter == 313)
				|| (region == CPU.PAL && status.interlace == true && status.vcounter == 312 && status.field == true))
		{
			status.vcounter = 0;
			status.field = !status.field;
		}
		if (scanline != null)
		{
			scanline.run();
		}
	}
}
