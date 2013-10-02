/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.memory;

import jario.hardware.Hardware;

public class UnmappedMemory extends Memory
{
	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
	}

	@Override
	public int size()
	{
		return 16 * 1024 * 1024;
	}

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
