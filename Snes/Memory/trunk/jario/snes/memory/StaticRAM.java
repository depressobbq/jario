package jario.snes.memory;

import jario.hardware.Hardware;

import java.util.Arrays;

public class StaticRAM extends Memory
{
	private byte[] data_;
	private int size_;

	public StaticRAM(int n)
	{
		size_ = n;
		data_ = new byte[size_];
	}

	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
		Arrays.fill(data_, (byte) 0);
	}

	@Override
	public int size()
	{
		return size_;
	}

	@Override
	public byte read8bit(int addr)
	{
		return data_[addr];
	}

	@Override
	public void write8bit(int addr, byte n)
	{
		data_[addr] = n;
	}
}
