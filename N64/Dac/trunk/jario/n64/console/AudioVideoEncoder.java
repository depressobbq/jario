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
 */

package jario.n64.console;

import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioVideoEncoder implements Hardware, Bus32bit, Configurable
{
	private static final int VIDEO_FPS_REG = 0;

	private static final int AUDIO_ENABLE_REG = 0;
	private static final int AUDIO_SAMPLE_RATE_REG = 1;
	private static final int AUDIO_BIT_LEN_REG = 2;
	private static final int AUDIO_CHANNELS_REG = 3;
	private static final int AUDIO_BUF_POS_REG = 4;

	private ExecutorService audioThreadExecutor;
	private int audioSampleRate;
	private int audioBitLength;
	private int audioChannels;

	private ExecutorService videoThreadExecutor;
	private Component drawSurface;
	private Graphics2D graphics;
	private BufferedImage bufferedImage;
	private int[] convet16to32 = new int[65536];

	private int audioDmaOffset;
	private int videoDmaOffset;

	private Bus32bit video;
	private Bus32bit audio;
	private Hardware rdram;

	public class AudioProcessThread implements Runnable
	{
		ByteBuffer buff;

		public AudioProcessThread(BusDMA source, int offset, int length)
		{
			buff = ByteBuffer.allocate(length);
			source.readDMA(offset, buff, 0, length);
		}

		public void run()
		{
			((BusDMA) audio).writeDMA(0, buff, 0, buff.array().length);
		}
	};

	public class VideoProcessThread implements Runnable
	{
		byte[] buffer;
		ByteBuffer buff;

		public VideoProcessThread(BusDMA source, int offset, int length)
		{
			buff = ByteBuffer.allocate(length);
			buffer = buff.array();
			source.readDMA(offset, buff, 0, length);
		}

		public void run()
		{
			if (bufferedImage == null || graphics == null)
				return;

			int w = (int) Math.sqrt((buffer.length << 1) / 3);
			int h = ((w >> 2) * 3);
			int offset = 0;

			for (int y = 0; y < h; y++)
			{
				for (int x = 0; x < w; x += 2)
				{
					bufferedImage.setRGB(x + 1, y, convet16to32[((buffer[offset] & 0xFF) << 8) | ((buffer[offset + 1] & 0xFF))]);
					bufferedImage.setRGB(x, y, convet16to32[((buffer[offset + 2] & 0xFF) << 8) | ((buffer[offset + 3] & 0xFF))]);
					offset += 4;
				}
			}

			graphics.drawImage(bufferedImage, 0, 0, null);
		}
	};

	public AudioVideoEncoder()
	{
		audioThreadExecutor = Executors.newSingleThreadExecutor();
		videoThreadExecutor = Executors.newSingleThreadExecutor();

		int red, green, blue, alpha;
		int color16;
		int color32;
		for (int count = 0; count < 65536; count++)
		{
			color16 = count;
			red = (color16 & 0xF800) >> 11;
			green = (color16 & 0x07C0) >> 6;
			blue = (color16 & 0x003E) >> 1;
			alpha = (color16 & 0x0001);
			color32 = (alpha << 27) | (red << 19) | (green << 11) | (blue << 3);
			convet16to32[count] = color32;
		}
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0: // video
			video = (Bus32bit) bus;
			if (video != null && drawSurface != null)
			{
				((Configurable) video).writeConfig("screen", drawSurface);
				graphics = (Graphics2D) drawSurface.getGraphics();
			}
			break;
		case 1: // audio
			if (bus == null)
				audioThreadExecutor.shutdownNow();
			audio = (Bus32bit) bus;
			if (audio != null)
			{
				audio.write32bit(AUDIO_SAMPLE_RATE_REG, audioSampleRate);
				audio.write32bit(AUDIO_BIT_LEN_REG, audioBitLength);
				audio.write32bit(AUDIO_CHANNELS_REG, audioChannels);
			}
			break;
		case 2: // reserved
			break;
		case 3: // reserved
			break;
		case 4: // reserved
			break;
		case 5: // reserved
			break;
		case 6:
			rdram = bus;
			break;
		}
	}

	@Override
	public void reset()
	{
	}

	@Override
	public int read32bit(int reg)
	{
		switch (reg)
		{
		case 0: // play position
			return audio != null ? audio.read32bit(AUDIO_BUF_POS_REG) : -1;
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
			if (audio != null)
				audio.write32bit(AUDIO_ENABLE_REG, value);
			break;
		case 1:
			audioSampleRate = value;
			if (audio != null)
				audio.write32bit(AUDIO_SAMPLE_RATE_REG, value);
			break;
		case 2:
			audioBitLength = value;
			if (audio != null)
				audio.write32bit(AUDIO_BIT_LEN_REG, value);
			break;
		case 3:
			audioChannels = value;
			if (audio != null)
				audio.write32bit(AUDIO_CHANNELS_REG, value);
			break;
		case 4: // viWidth
			if (value != 0)
				bufferedImage = new BufferedImage(value, (value >> 2) * 3, BufferedImage.TYPE_INT_RGB);
			break;
		case 5: // fps
			if (video != null)
				video.write32bit(VIDEO_FPS_REG, value);
			break;
		case 6:
			audioDmaOffset = value;
			break;
		case 7: // audio dma
			if (audio != null)
			{
				audioThreadExecutor.execute(new AudioProcessThread((BusDMA) rdram, audioDmaOffset, value));
			}
			break;
		case 8:
			videoDmaOffset = value;
			break;
		case 9: // video dma
			videoThreadExecutor.execute(new VideoProcessThread((BusDMA) rdram, videoDmaOffset, value));
			break;
		}
	}

	@Override
	public Object readConfig(String key)
	{
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("screen")) drawSurface = (Component) value;
	}
}
