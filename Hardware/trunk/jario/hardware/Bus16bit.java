/**
 * Copyright 2013 Jason LaDere
 *
 * Component Plugin v1.1
 */

package jario.hardware;

public interface Bus16bit
{
	public short read16bit(int address);
	public void write16bit(int address, short data);
}
