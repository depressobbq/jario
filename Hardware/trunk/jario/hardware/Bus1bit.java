/**
 * Copyright 2013 Jason LaDere
 *
 * Component Plugin v1.1
 */

package jario.hardware;

public interface Bus1bit
{
	public boolean read1bit(int address);
	public void write1bit(int address, boolean data);
}
