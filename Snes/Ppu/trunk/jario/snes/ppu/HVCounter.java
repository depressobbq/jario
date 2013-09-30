package jario.snes.ppu;

public class HVCounter
{
	class Status
	{
		public boolean interlace;
		public boolean field;
		public int vcounter;
		public int hcounter;
	}
	
	private PPU ppu;
	private Status status = new Status();
	int region;
	
	public HVCounter(PPU ppu)
	{
		this.ppu = ppu;
	}

	final void tick(int clocks)
	{
		status.hcounter += clocks;
		if (status.hcounter >= lineclocks())
		{
			status.hcounter -= lineclocks();
			vcounter_tick();
		}
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

	final int hdot()
	{
		if (region == PPU.NTSC && !status.interlace && status.vcounter == 240 && status.field)
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
		if (region == PPU.NTSC && !status.interlace && status.vcounter == 240 && status.field) { return 1360; }
		return 1364;
	}

	void reset()
	{
		status.interlace = false;
		status.field = false;
		status.vcounter = 0;
		status.hcounter = 0;
	}

	private void vcounter_tick()
	{
		if (++status.vcounter == 128)
		{
			status.interlace = ppu.display.interlace;
		}

		if ((region == PPU.NTSC && status.interlace == false && status.vcounter == 262)
				|| (region == PPU.NTSC && status.interlace == true && status.vcounter == 263)
				|| (region == PPU.NTSC && status.interlace == true && status.vcounter == 262 && status.field == true)
				|| (region == PPU.PAL && status.interlace == false && status.vcounter == 312)
				|| (region == PPU.PAL && status.interlace == true && status.vcounter == 313)
				|| (region == PPU.PAL && status.interlace == true && status.vcounter == 312 && status.field == true))
		{
			status.vcounter = 0;
			status.field = !status.field;
		}
	}
}
