/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.audio;

import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Hardware;

import java.nio.ByteBuffer;

public class Audio implements Hardware, Bus32bit
{
	private BusDMA output;
	private byte[] buffer0 = new byte[8192];
	private ByteBuffer outputBuffer;
	private int bufferIndex;

	public Audio()
	{
		outputBuffer = ByteBuffer.wrap(buffer0);
		reset();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			output = (BusDMA) hw;
			break;
		}
	}

	@Override
	public void reset()
	{
		bufferIndex = 0;
	}

	@Override
	public int read32bit(int address)
	{
		switch (address)
		{
		case 0:
			int index = bufferIndex;
			output.writeDMA(0, outputBuffer, 0, index);
			bufferIndex = 0;
			return index;
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int address, int sample)
	{
		if (bufferIndex < buffer0.length)
		{
			buffer0[bufferIndex++] = (byte) (sample >> 24); // left
			buffer0[bufferIndex++] = (byte) (sample >> 16); // left
			buffer0[bufferIndex++] = (byte) (sample >> 8); // right
			buffer0[bufferIndex++] = (byte) (sample >> 0); // right
		}
	}
}
