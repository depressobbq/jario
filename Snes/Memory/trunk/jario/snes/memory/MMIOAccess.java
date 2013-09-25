package jario.snes.memory;

import jario.hardware.Bus8bit;
import jario.hardware.Hardware;

public class MMIOAccess extends Memory
{
	private Bus8bit[] _mmio_read = new Bus8bit[0x8000];
	private Bus8bit[] _mmio_write = new Bus8bit[0x8000];

	public MMIOAccess(UnmappedMMIO mmio_unmapped)
	{
		for (int i = 0; i < 0x8000; i++)
		{
			_mmio_read[i] = mmio_unmapped;
			_mmio_write[i] = mmio_unmapped;
		}
	}

	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
	}

	@Override
	public final byte read8bit(int addr)
	{
		return _mmio_read[addr & 0x7fff].read8bit(addr);
	}

	@Override
	public final void write8bit(int addr, byte data)
	{
		_mmio_write[addr & 0x7fff].write8bit(addr, data);
	}

	void map(int addr, Bus8bit access_read, Bus8bit access_write)
	{
		if (access_read != null)
		{
			_mmio_read[addr & 0x7fff] = access_read;
		}
		if (access_write != null)
		{
			_mmio_write[addr & 0x7fff] = access_write;
		}
	}
}
