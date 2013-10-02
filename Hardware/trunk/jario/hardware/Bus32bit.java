/**
 * Copyright 2013 Jason LaDere
 *
 * Component Plugin v1.1
 */

package jario.hardware;

public interface Bus32bit
{
	public int read32bit(int address);
	public void write32bit(int address, int data);
}
