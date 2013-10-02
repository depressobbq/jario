/**
 * Copyright 2013 Jason LaDere
 *
 * Component Plugin v1.1
 */

package jario.hardware;

import java.nio.ByteBuffer;

public interface BusDMA
{
	public void readDMA(int address, ByteBuffer data, int offset, int length);
	public void writeDMA(int address, ByteBuffer data, int offset, int length);
}
