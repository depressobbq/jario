/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.memory;

import jario.hardware.Bus1bit;
import jario.hardware.Bus8bit;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

public class MemoryBus extends Bus implements Hardware, Bus1bit, Configurable
{
	private StaticRAM wram;
	private MMIOAccess mmio;
	private UnmappedMMIO mmio_unmapped;

	static Bus8bit cpu;
	private Bus8bit ppu;
	private Configurable cartridge;

	private byte wram_init_value;

	public MemoryBus()
	{
		wram = new StaticRAM(128 * 1024);
		mmio_unmapped = new UnmappedMMIO();
		mmio = new MMIOAccess(mmio_unmapped);
		map_reset();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			cpu = (Bus8bit) hw;
			break;
		case 1:
			ppu = (Bus8bit) hw;
			break;
		case 2:
			cartridge = (Configurable) hw;
			load_cart();
			for (int i = 0x2100; i <= 0x213f; i++)
			{
				mmio.map(i, ppu);
			}
			for (int i = 0x2140; i <= 0x217f; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x2180; i <= 0x2183; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x4016; i <= 0x4017; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x4200; i <= 0x421f; i++)
			{
				mmio.map(i, cpu);
			}
			for (int i = 0x4300; i <= 0x437f; i++)
			{
				mmio.map(i, cpu);
			}
			break;
		}
	}

	@Override
	public void reset()
	{
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("mmio")) return mmio;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("wram init value"))
		{
			wram_init_value = (Byte) value;
			power();
		}
		else if (key.equals("map"))
		{
			Object[] params = (Object[])value;
			map(MapMode.values()[(Integer)params[0]], (Integer)params[1], (Integer)params[2], (Integer)params[3],
					(Integer)params[4], (Bus8bit)params[5], (Integer)params[6], (Integer)params[7]);
		}
	}
	
	private void load_cart()
	{
		map_reset();
		map_xml();
		map_system();
	}

	private void power()
	{
		for (int n = 0; n < (128 * 1024); n++)
		{
			wram.write8bit(n, wram_init_value);
		}
	}

	private void map_reset()
	{
		map(MapMode.Direct, 0x00, 0xff, 0x0000, 0xffff, memory_unmapped);
		map(MapMode.Direct, 0x00, 0x3f, 0x2000, 0x5fff, mmio);
		map(MapMode.Direct, 0x80, 0xbf, 0x2000, 0x5fff, mmio);
		for (int i = 0x2000; i <= 0x5fff; i++)
		{
			mmio.map(i, mmio_unmapped);
		}
	}

	private void map_xml()
	{
		if (cartridge != null)
		{
			cartridge.writeConfig("mapmem", this);
		}
	}

	private void map_system()
	{
		map(MapMode.Linear, 0x00, 0x3f, 0x0000, 0x1fff, wram, 0x000000, 0x002000);
		map(MapMode.Linear, 0x80, 0xbf, 0x0000, 0x1fff, wram, 0x000000, 0x002000);
		map(MapMode.Linear, 0x7e, 0x7f, 0x0000, 0xffff, wram);
	}

	@Override
	public boolean read1bit(int address)
	{
		return false;
	}

	@Override
	public void write1bit(int address, boolean data)
	{
		if (address == 0) ((Bus1bit)cpu).write1bit(0, data); // irq
	}
}
