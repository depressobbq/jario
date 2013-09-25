package jario.snes.memory;

import jario.hardware.Bus8bit;
import jario.hardware.Hardware;

public abstract class Memory implements Hardware, Bus8bit
{
	public int size()
	{
		return 0;
	}
}
