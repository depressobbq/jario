/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.memory;

import jario.hardware.Bus8bit;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

public class MMIOAccess implements Hardware, Bus8bit, Configurable
{
	private Bus8bit[] mmio = new Bus8bit[0x8000];

	public MMIOAccess(UnmappedMMIO mmio_unmapped)
	{
		for (int i = 0; i < 0x8000; i++)
		{
			mmio[i] = mmio_unmapped;
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
		return mmio[addr & 0x7fff].read8bit(addr);
	}

	@Override
	public final void write8bit(int addr, byte data)
	{
		mmio[addr & 0x7fff].write8bit(addr, data);
	}
	
	@Override
	public Object readConfig(String key)
	{
		if (key.equals("size")) return 0;
		// last
		try { return handle(Integer.parseInt(key, 16)); } catch (NumberFormatException e) { return null; }
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		// last
		try { map(Integer.parseInt(key, 16), (Bus8bit)value); } catch (NumberFormatException e) { }
	}
	
	Bus8bit handle(int addr)
	{
		return mmio[addr & 0x7fff];
	}
	
	void map(int addr, Bus8bit access)
	{
		mmio[addr & 0x7fff] = access;
	}
}
