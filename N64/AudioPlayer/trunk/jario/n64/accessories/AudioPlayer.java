/**
 * Copyright 2009, 2013 Jason LaDere
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Originally based on Project64 code.
 *
 */

package jario.n64.accessories;

import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class AudioPlayer implements Hardware, Bus32bit, BusDMA, Configurable
{
	private static final int DEFAULT_AUDIO_SAMPLE_RATE = 44100;
	private static final int DEFAULT_AUDIO_BIT_LENGTH = 16;
	private static final int DEFAULT_AUDIO_CHANNELS = 2;

	private AudioFormat audioFormat;
	private SourceDataLine audioDataLine;
	private int audioSampleRate;
	private int audioBitLength;
	private int audioChannels;
	private boolean changed;
	private boolean audioOn = true;

	public AudioPlayer()
	{
		audioSampleRate = DEFAULT_AUDIO_SAMPLE_RATE;
		audioBitLength = DEFAULT_AUDIO_BIT_LENGTH;
		audioChannels = DEFAULT_AUDIO_CHANNELS;
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void reset()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int read32bit(int reg)
	{
		switch (reg)
		{
		case 0:
			return audioOn ? 1 : 0;
		case 1:
			return audioSampleRate;
		case 2:
			return audioBitLength;
		case 3:
			return audioChannels;
		case 4: // play position
			if (audioDataLine != null)
				return audioDataLine.getFramePosition() << 2;
			else
				return -1;
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch (reg)
		{
		case 0: // audio on/off
			audioOn = value != 0;
			break;
		case 1:
			audioSampleRate = value;
			changed = true;
			break;
		case 2:
			audioBitLength = value;
			changed = true;
			break;
		case 3:
			audioChannels = value;
			changed = true;
			break;
		}
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("enable")) return audioOn;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("enable")) audioOn = (Boolean) value;
	}

	@Override
	public void readDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void writeDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		if (audioOn && audioDataLine == null)
		{
			initAudio();
		}
		if (!audioOn && audioDataLine != null)
		{
			closeAudio();
		}
		if (changed)
		{
			initAudio();
			changed = false;
		}
		if (audioDataLine != null)
		{
			audioDataLine.write(dma.array(), offset, length);
		}
	}

	private boolean initAudio()
	{
		closeAudio(); // Release just in case...
		audioFormat = new AudioFormat(audioSampleRate, audioBitLength, audioChannels, true, true);
		SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
		try
		{
			audioDataLine = (SourceDataLine) AudioSystem.getLine(info);
			audioDataLine.open(audioFormat, 8192);
			audioDataLine.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void closeAudio()
	{
		if (audioDataLine != null)
		{
			audioDataLine.stop();
			audioDataLine.close();
			audioDataLine = null;
		}
	}
}
