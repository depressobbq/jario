/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.input;

import jario.snes.input.Input.Device;

public class Port
{
	public Device device;
	public int counter0; // read counters
	public int counter1;

	public Superscope superscope = new Superscope();
	public Justifier justifier = new Justifier();
}
