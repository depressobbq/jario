/**
 * Copyright 2013 Jason LaDere
 *
 * Component Plugin v1.1
 */

package jario.hardware;

public interface Bus64bit
{
	public long read64bit(int address);
	public void write64bit(int address, long data);
}
