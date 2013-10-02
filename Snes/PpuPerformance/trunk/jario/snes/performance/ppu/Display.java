/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.ppu;

public class Display
{
	public boolean interlace;
	public boolean overscan;
	public int width;
	public int height;
	// public int frameskip;
	// public int framecounter;

	int latch;
}
