/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.cartridge;

import jario.hardware.Bus8bit;
import jario.snes.cartridge.Cartridge.MapMode;

public class Mapping
{
	public MappedRAM memory;
	public Bus8bit mmio;
	public MapMode mode;
	public int banklo;
	public int bankhi;
	public int addrlo;
	public int addrhi;
	public int offset;
	public int size;

	public Mapping()
	{
		memory = null;
		mmio = null;
		mode = MapMode.Direct;
		banklo = bankhi = addrlo = addrhi = offset = size = 0;
	}

	public Mapping(MappedRAM memory_)
	{
		memory = memory_;
		mmio = null;
		mode = MapMode.Direct;
		banklo = bankhi = addrlo = addrhi = offset = size = 0;
	}

	public Mapping(Bus8bit mmio_)
	{
		memory = null;
		mmio = mmio_;
		mode = MapMode.Direct;
		banklo = bankhi = addrlo = addrhi = offset = size = 0;
	}
}
