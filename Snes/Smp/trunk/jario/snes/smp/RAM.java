package jario.snes.smp;

import jario.hardware.Bus8bit;
import jario.hardware.Hardware;

import java.util.Arrays;

public class RAM implements Hardware, Bus8bit
{
	private byte[] ram;

	public RAM()
	{
		ram = new byte[64 * 1024];
	}

	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
		Arrays.fill(ram, (byte) 0);
	}

	@Override
	public byte read8bit(int address)
	{
		return ram[address];
	}

	@Override
	public void write8bit(int address, byte data)
	{
		ram[address] = data;
	}
}
