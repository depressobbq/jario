/**
 * Copyright 2013 Jason LaDere
 *
 * Component Plugin v1.1
 */

package jario.hardware;

public interface Bus8bit
{
	public byte read8bit(int address);
	public void write8bit(int address, byte data);
}
