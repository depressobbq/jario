/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

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
