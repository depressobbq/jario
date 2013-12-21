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

public class Bus implements Hardware, Bus8bit, Configurable
{
	private StaticRAM wram;
	private MMIOAccess mmio;
	private UnmappedMemory memory_unmapped;
	private UnmappedMMIO mmio_unmapped;

	static Bus8bit cpu;
	private Bus8bit ppu;
	private Bus8bit cartridge;

	public enum MapMode
	{
		Direct, Linear, Shadow
	}

	public Page[] page = new Page[65536];
	private byte wram_init_value;

	public Bus()
	{
		for (int i = 0; i < page.length; i++)
		{
			page[i] = new Page();
		}
		wram = new StaticRAM(128 * 1024);
		memory_unmapped = new UnmappedMemory();
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
			cartridge = (Bus8bit) hw;
			power();
			break;
		}
	}

	@Override
	public void reset()
	{
	}

	@Override
	public final byte read8bit(int addr)
	{
		addr &= 0x00FFFFFF;
		Page p = page[addr >> 8];
		return p.access.read8bit(p.offset + addr);
	}

	@Override
	public final void write8bit(int addr, byte data)
	{
		addr &= 0x00FFFFFF;
		Page p = page[addr >> 8];
		p.access.write8bit(p.offset + addr, data);
	}

	@Override
	public Object readConfig(String key)
	{
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("wram init value")) wram_init_value = (Byte) value;
	}

	private int mirror(int addr, int size)
	{
		int base_ = 0;
		if (size != 0)
		{
			int mask = 1 << 23;
			while (addr >= size)
			{
				while ((addr & mask) == 0)
				{
					mask >>= 1;
				}
				addr -= mask;
				if (size > mask)
				{
					size -= mask;
					base_ += mask;
				}
				mask >>= 1;
			}
			base_ += addr;
		}
		return base_;
	}

	private void map(int addr, Bus8bit access, int offset)
	{
		Page p = page[addr >> 8];
		p.access = access;
		p.offset = offset - addr;
	}

	private void map(MapMode mode, int bank_lo, int bank_hi, int addr_lo, int addr_hi, Bus8bit access)
	{
		map(mode, bank_lo, bank_hi, addr_lo, addr_hi, access, 0, 0);
	}

	private void map(MapMode mode, int bank_lo, int bank_hi, int addr_lo, int addr_hi, Bus8bit access, int offset, int size)
	{
		assert bank_lo <= bank_hi;
		assert addr_lo <= addr_hi;

		int page_lo = (addr_lo >> 8) & 0xFF;
		int page_hi = (addr_hi >> 8) & 0xFF;
		int index = 0;

		switch (mode)
		{
		case Direct:
		{
			for (int bank = bank_lo; bank <= bank_hi; bank++)
			{
				for (int page = page_lo; page <= page_hi; page++)
				{
					map((bank << 16) + (page << 8), access, (bank << 16) + (page << 8));
				}
			}
		}
			break;
		case Linear:
		{
			for (int bank = bank_lo; bank <= bank_hi; bank++)
			{
				for (int page = page_lo; page <= page_hi; page++)
				{
					map((bank << 16) + (page << 8), access, mirror(offset + index, ((Memory) access).size()));
					index += 256;
					if (size != 0)
					{
						index %= size;
					}
				}
			}
		}
			break;
		case Shadow:
		{
			for (int bank = bank_lo; bank <= bank_hi; bank++)
			{
				index += (page_lo * 256);
				if (size != 0)
				{
					index %= size;
				}

				for (int page = page_lo; page <= page_hi; page++)
				{
					map((bank << 16) + (page << 8), access, mirror(offset + index, ((Memory) access).size()));
					index += 256;
					if (size != 0)
					{
						index %= size;
					}
				}

				index += ((255 - page_hi) * 256);
				if (size != 0)
				{
					index %= size;
				}
			}
		}
			break;
		}
	}

	private void power()
	{
		map_reset();
		map_xml();
		map_system();

		for (int i = 0; i < (128 * 1024); i++)
		{
			wram.write8bit(i, wram_init_value);
		}

		for (int i = 0x2100; i <= 0x2103; i++)
		{
			mmio.map(i, null, ppu);
		}
		for (int i = 0x2104; i <= 0x2106; i++)
		{
			mmio.map(i, ppu, ppu);
		}
		for (int i = 0x2107; i <= 0x2107; i++)
		{
			mmio.map(i, null, ppu);
		}
		for (int i = 0x2108; i <= 0x210a; i++)
		{
			mmio.map(i, ppu, ppu);
		}
		for (int i = 0x210b; i <= 0x2113; i++)
		{
			mmio.map(i, null, ppu);
		}
		for (int i = 0x2114; i <= 0x2116; i++)
		{
			mmio.map(i, ppu, ppu);
		}
		for (int i = 0x2117; i <= 0x2117; i++)
		{
			mmio.map(i, null, ppu);
		}
		for (int i = 0x2118; i <= 0x211a; i++)
		{
			mmio.map(i, ppu, ppu);
		}
		for (int i = 0x211b; i <= 0x2123; i++)
		{
			mmio.map(i, null, ppu);
		}
		for (int i = 0x2124; i <= 0x2126; i++)
		{
			mmio.map(i, ppu, ppu);
		}
		for (int i = 0x2127; i <= 0x2127; i++)
		{
			mmio.map(i, null, ppu);
		}
		for (int i = 0x2128; i <= 0x212a; i++)
		{
			mmio.map(i, ppu, ppu);
		}
		for (int i = 0x212b; i <= 0x2133; i++)
		{
			mmio.map(i, null, ppu);
		}
		for (int i = 0x2134; i <= 0x213f; i++)
		{
			mmio.map(i, ppu, null);
		}

		for (int i = 0x2140; i <= 0x217f; i++)
		{
			mmio.map(i, cpu, cpu);
		}
		for (int i = 0x2180; i <= 0x2183; i++)
		{
			mmio.map(i, cpu, cpu);
		}
		for (int i = 0x4016; i <= 0x4017; i++)
		{
			mmio.map(i, cpu, cpu);
		}
		for (int i = 0x4200; i <= 0x421f; i++)
		{
			mmio.map(i, cpu, cpu);
		}
		for (int i = 0x4300; i <= 0x437f; i++)
		{
			mmio.map(i, cpu, cpu);
		}
	}

	private void map_reset()
	{
		map(MapMode.Direct, 0x00, 0xff, 0x0000, 0xffff, memory_unmapped);
		map(MapMode.Direct, 0x00, 0x3f, 0x2000, 0x5fff, mmio);
		map(MapMode.Direct, 0x80, 0xbf, 0x2000, 0x5fff, mmio);
		for (int i = 0x2000; i <= 0x5fff; i++)
		{
			mmio.map(i, mmio_unmapped, mmio_unmapped);
		}
	}

	private void map_xml()
	{
		if (cartridge != null)
		{
			map(MapMode.Direct, 0x00, 0x3f, 0x6000, 0xffff, cartridge);
			map(MapMode.Direct, 0x40, 0x7d, 0x0000, 0xffff, cartridge);
			map(MapMode.Direct, 0x80, 0xbf, 0x6000, 0xffff, cartridge);
			map(MapMode.Direct, 0xc0, 0xfe, 0x0000, 0xffff, cartridge);
			map(MapMode.Direct, 0xff, 0xff, 0x0000, 0xfeff, cartridge);
		}
	}

	private void map_system()
	{
		map(MapMode.Linear, 0x00, 0x3f, 0x0000, 0x1fff, wram, 0x000000, 0x002000);
		map(MapMode.Linear, 0x80, 0xbf, 0x0000, 0x1fff, wram, 0x000000, 0x002000);
		map(MapMode.Linear, 0x7e, 0x7f, 0x0000, 0xffff, wram);
	}
}
