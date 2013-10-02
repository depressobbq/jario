/**
 * Copyright 2005, 2013 Jason LaDere
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

package jario.n64.console.rcp;

import jario.hardware.Bus32bit;
import jario.hardware.Hardware;

public class AudioInterface implements Hardware, Bus32bit
{
	private static final int SYSTEM_NTSC = 0;
	private static final int SYSTEM_PAL = 1;
	private static final int SYSTEM_MPAL = 2;

	private static final int AI_DRAM_ADDR_REG = 0;
	private static final int AI_LEN_REG = 1;
	private static final int AI_CONTROL_REG = 2;
	private static final int AI_STATUS_REG = 3;
	private static final int AI_DACRATE_REG = 4;
	private static final int AI_BITRATE_REG = 5;

	private static final int MI_INTR_REG = 0x04300008;

	private static final int MI_INTR_CLR_AI = 0x0010; /* Bit 4: clear AI interrupt */
	private static final int MI_INTR_SET_AI = 0x0020; /* Bit 5: set AI interrupt */
	// private static final int MI_INTR_MASK_AI = 0x04; /* Bit 2: AI intr mask */
	// private static final int MI_INTR_AI = 0x04; /* Bit 2: AI intr */

	private static final int SND_IS_NOT_EMPTY = 0x4000000;
	private static final int SND_IS_FULL = 0x8000000;
	private static final int SEGMENTS = 3;
	// private static final int LOCK_SIZE = 0x1000; // LOCKSIZE must not be fractional
	// private static final int MAXBUFFER = (LOCK_SIZE * SEGMENTS + LOCK_SIZE);

	private static final int DAC_AUDIO_POS_REG = 0;
	private static final int DAC_SAMPLE_RATE_REG = 1;
	private static final int DAC_BIT_LENGTH_REG = 2;
	private static final int DAC_CHANNELS_REG = 3;
	private static final int DAC_DMA_OFFSET_REG = 6;
	private static final int DAC_DMA_REG = 7;

	private int readLoc;
	private int writeLoc;
	private int SampleRate;
	private int SegmentSize;
	private int write_pos = 0, play_pos = 0;
	private int last_write = -1;
	// private int dwBytes1;
	private int buffsize = 0;
	private int laststatus = 0;
	private int dacrate = 0;

	private int[] regAI = new int[6];

	private Hardware rdram;
	private Bus32bit mi;
	private Bus32bit dac;

	public AudioInterface()
	{
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			rdram = bus;
			break;
		case 1:
			mi = (Bus32bit) bus;
			break;
		case 2:
			dac = (Bus32bit) bus;
			if (dac != null)
				dac.write32bit(0, 1);
			break;
		case 3: // timing
			break;
		}
	}

	@Override
	public void reset()
	{
		dacrate = 0;
	}

	@Override
	public int read32bit(int reg)
	{
		switch ((reg - 0x04500000) >> 2)
		{
		case 1:
			if (buffsize == 0)
			{
				regAI[AI_LEN_REG] = 0;
				return regAI[AI_LEN_REG];
			}
			updateStatus();
			return regAI[AI_LEN_REG];
		case 3:
			return regAI[AI_STATUS_REG];
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch ((reg - 0x04500000) >> 2)
		{
		case 0:
			regAI[AI_DRAM_ADDR_REG] = value;
			break;
		case 1:
			regAI[AI_LEN_REG] = value;
			addBuffer(rdram, regAI[AI_DRAM_ADDR_REG] & 0x00FFFFF8, regAI[AI_LEN_REG] & 0x3FFF8);
			break;
		case 2:
			regAI[AI_CONTROL_REG] = (value & 1);
			break;
		case 3:
			mi.write32bit(MI_INTR_REG, MI_INTR_CLR_AI);
			break;
		case 4:
			regAI[AI_DACRATE_REG] = value;
			int systemType = SYSTEM_NTSC;
			if (dacrate != regAI[AI_DACRATE_REG])
			{
				dacrate = regAI[AI_DACRATE_REG];
				float frequency = 0.0f;
				switch (systemType)
				{
				case SYSTEM_NTSC:
					frequency = 48681812.0f / (dacrate + 1);
					break;
				case SYSTEM_PAL:
					frequency = 49656530.0f / (dacrate + 1);
					break;
				case SYSTEM_MPAL:
					frequency = 48628316.0f / (dacrate + 1);
					break;
				}
				SampleRate = (int)(frequency * 1.04166667f); // * 1.04166667f to compensate for 62.5 fps
				SegmentSize = 0; // Trash it... we need to redo the Frequency anyway...
			}
			break;
		case 5:
			regAI[AI_BITRATE_REG] = value;
			break;
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void updateStatus()
	{
		play_pos = dac.read32bit(DAC_AUDIO_POS_REG);

		if (play_pos == -1)
			return;

		if (play_pos < buffsize)
		{
			write_pos = (buffsize * SEGMENTS) - buffsize;
		}
		else
		{
			write_pos = ((play_pos / buffsize) * buffsize) - buffsize;
		}

		int writediff = (write_pos - last_write);

		if (writediff < 0)
		{
			writediff += SEGMENTS * buffsize;
		}

		int play_seg = play_pos / buffsize;
		int write_seg = play_pos / buffsize;
		int last_seg = last_write / buffsize;

		if (last_seg == write_seg)
		{ // The FIFO is still full and DMA is busy...
			regAI[AI_STATUS_REG] |= 0xC0000000;
			regAI[AI_LEN_REG] = buffsize - (play_pos - ((play_pos / buffsize) * buffsize));
			if (play_pos > write_pos)
			{
				regAI[AI_LEN_REG] = buffsize - ((write_pos - play_pos) - buffsize);
			}
			else
			{
				regAI[AI_LEN_REG] = (buffsize - play_pos);
			}
			regAI[AI_LEN_REG] += buffsize;
			laststatus = 0xC0000000;
			return;
		}
		if (laststatus == 0xC0000000)
		{ // Then we need to generate an interrupt now...
			regAI[AI_LEN_REG] = buffsize;
			regAI[AI_LEN_REG] = buffsize - (play_pos - ((play_pos / buffsize) * buffsize));
			regAI[AI_STATUS_REG] = 0x40000000; // DMA is still busy...
			if (((play_seg - last_seg) & 7) > 3)
			{
				mi.write32bit(MI_INTR_REG, MI_INTR_SET_AI);
			}
			laststatus = 0x40000000;
			return;
		}
		if (laststatus == 0x40000000)
		{
			if (writediff > (int) (buffsize * 2))
			{ // This means we are doing a buffer underrun... damnit!
				regAI[AI_LEN_REG] = 0;
				regAI[AI_STATUS_REG] = 0x00000000; // DMA is still busy...
				if (((play_seg - last_seg) & 7) > 2)
				{
					mi.write32bit(MI_INTR_REG, MI_INTR_SET_AI);
				}
			}
			return;
		}
	}

	// Fills up a buffer and remixes the audio
	private void fillBuffer(Hardware buff, int offset, int len)
	{
		int write_seg = 0;
		int last_seg = 0;
		buffsize = len; // Save it globally

		play_pos = dac.read32bit(DAC_AUDIO_POS_REG);

		if (play_pos < len)
		{
			write_pos = (len * SEGMENTS) - len;
		}
		else
		{
			write_pos = ((play_pos / len) * len) - len;
		}

		if (last_write == -1)
		{
			last_write = (write_pos - (2 * len)); // Back up 2 segments...
			if (last_write < 0)
			{
				last_write += (SEGMENTS * len);
			}
		}

		if (last_write == write_pos)
		{ // Then we must freeze...

		}

		last_seg = (last_write / len);
		write_seg = (write_pos / len);

		if (last_seg == ((write_seg - 2) & 0x7))
		{ // Means first buffer is clear to write to...
			write_pos = (last_write + len);
			if (write_pos >= len * SEGMENTS)
			{
				write_pos -= (len * SEGMENTS);
			}
			// Set DMA Busy
			last_write += len;
			if (last_write >= len * SEGMENTS)
			{
				last_write -= (len * SEGMENTS);
			}
			regAI[AI_STATUS_REG] |= 0x40000000;
			laststatus = 0x40000000;
		}
		else if (last_seg == ((write_seg - 1) & 0x7))
		{
			// Set DMA Busy
			// Set FIFO Buffer Full
			last_write = write_pos; // Lets get it back up to speed for audio accuracy...
			regAI[AI_STATUS_REG] |= 0xC0000000;
			laststatus = 0xC0000000;
		}
		else
		{ // We get here if our audio stream from the game is running TOO slow...
			last_write = write_pos; // Lets get it back up to speed for audio accuracy...
			regAI[AI_STATUS_REG] |= 0x00000000;
			laststatus = 0x00000000;
		}

		// dwBytes1 = len;

		dac.write32bit(DAC_DMA_OFFSET_REG, offset);
		dac.write32bit(DAC_DMA_REG, len);
	}

	private int addBuffer(Hardware start, int offset, int length)
	{
		int retVal = 0;

		if (length == 0) { return 0; }
		if (length == 0x8C0)
		{ // TODO: This proves I need more buffering!!!
			length = 0x840;
		}
		if (length == 0x880)
		{ // TODO: This proves I need more buffering!!!
			length = 0x840;
		}
		if (length != SegmentSize)
		{
			if (SampleRate != 0)
			{
				SegmentSize = length;
				dac.write32bit(DAC_SAMPLE_RATE_REG, SampleRate);
				dac.write32bit(DAC_BIT_LENGTH_REG, 16);
				dac.write32bit(DAC_CHANNELS_REG, 2);
			}
		}

		if (readLoc == writeLoc)
		{ // Reset our pointer if we can...
			writeLoc = readLoc = 0;
		}

		if (readLoc != writeLoc)
		{ // Then we have stuff in the buffer already... This is a double buffer
			retVal |= SND_IS_FULL;
		}

		retVal |= SND_IS_NOT_EMPTY; // Buffer is not empty...

		writeLoc += length;

		fillBuffer(start, offset, length);
		readLoc = writeLoc = 0;
		updateStatus();

		return retVal;
	}
}
