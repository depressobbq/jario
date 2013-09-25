package jario.snes.memory;

import jario.hardware.Bus8bit;

public class UnmappedMMIO implements Bus8bit
{
	@Override
	public byte read8bit(int addr)
	{
		return Bus.cpu.read8bit(0xFFFF);
	}

	@Override
	public void write8bit(int addr, byte data)
	{
	}
}
