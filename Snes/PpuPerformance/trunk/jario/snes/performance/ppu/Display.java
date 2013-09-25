package jario.snes.performance.ppu;

import jario.hardware.Bus8bit;
import jario.hardware.Hardware;

public class Display implements Hardware, Bus8bit
{
	public boolean interlace;
	public boolean overscan;
	public int width;
	public int height;
	// public int frameskip;
	// public int framecounter;

	int latch;

	private PPU ppu;

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			ppu = (PPU) hw;
			break;
		}
	}

	@Override
	public void reset()
	{
	}

	@Override
	public byte read8bit(int address)
	{
		switch (address)
		{
		case 0:
			return (byte) (interlace ? 1 : 0);
		case 1:
			return (byte) (overscan ? 1 : 0);
		case 2:
			return (byte) (ppu.hires() ? 1 : 0);
		case 3:
			return (byte) latch;
		}
		return 0;
	}

	@Override
	public void write8bit(int address, byte data)
	{
		if (latch != 0 && (data & 0x80) == 0)
		{
			ppu.latch_counters();
		}
		latch = (data >> 7) & 0x1;
	}
}
