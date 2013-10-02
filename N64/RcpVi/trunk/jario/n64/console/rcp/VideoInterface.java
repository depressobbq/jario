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
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.awt.Component;
import java.awt.Dimension;

public class VideoInterface implements Hardware, Clockable, Bus32bit, Configurable
{
	private int FRAMES_PER_SECOND = 60;
	private int SKIP_TICKS = 1000 / FRAMES_PER_SECOND;
	
	private static final int VI_STATUS_REG = 0;
	private static final int VI_ORIGIN_REG = 1;
	private static final int VI_WIDTH_REG = 2;
	private static final int VI_INTR_REG = 3;
	private static final int VI_CURRENT_REG = 4;
	private static final int VI_BURST_REG = 5;
	private static final int VI_V_SYNC_REG = 6;
	private static final int VI_H_SYNC_REG = 7;
	private static final int VI_LEAP_REG = 8;
	private static final int VI_H_START_REG = 9;
	private static final int VI_V_START_REG = 10;
	private static final int VI_V_BURST_REG = 11;
	private static final int VI_X_SCALE_REG = 12;
	private static final int VI_Y_SCALE_REG = 13;

	private static final int MI_INTR_REG = 0x04300008;

	private static final int DAC_WIDTH_BUF_REG = 4;
	private static final int DAC_FPS_REG = 5;

	private static final int MI_INTR_CLR_VI = 0x0040; /* Bit 6: clear VI interrupt */
	private static final int MI_INTR_SET_VI = 0x0080; /* Bit 7: set VI interrupt */
	// private static final int MI_INTR_MASK_CLR_VI = 0x0040; /* Bit 6: clear VI mask */
	// private static final int MI_INTR_MASK_SET_VI = 0x0080; /* Bit 7: set VI mask */
	// private static final int MI_INTR_MASK_VI = 0x08; /* Bit 3: VI intr mask */
	// private static final int MI_INTR_VI = 0x08; /* Bit 3: VI intr */

	private static final int NUM_FRAMES = 7;

	private Component drawSurface;
	private boolean useFrameBuffer;
	private int fbLenBytes;
	private int[] regVI = new int[14];
	private long lastFrame;
	private long[] frames = new long[NUM_FRAMES];
	private int currentFrame;
	private long frequency;
	private int oldViVsyncReg = 0;
	private int viIntrTime = 500000;
	private int viFieldNumber;
	private long currentTime;
	private long next_game_tick;
	private long sleep_time;
	private boolean frameLimit = true;

	private Bus32bit mi;
	private Bus32bit dac;
	private Bus32bit timer;

	public VideoInterface()
	{
		frequency = 1000;
		currentFrame = 0;
		viFieldNumber = 0;
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0: // rdram
			break;
		case 1:
			mi = (Bus32bit) bus;
			break;
		case 2:
			dac = (Bus32bit) bus;
			if (dac != null && drawSurface != null)
			{
				((Configurable) dac).writeConfig("screen", drawSurface);
			}
			break;
		case 3:
			timer = (Bus32bit) bus;
			if (timer != null)
				timer.write32bit(3, 5000);
			break;
		}
	}

	@Override
	public void reset()
	{
		frequency = 1000;
		currentFrame = 0;
		viFieldNumber = 0;
	}

	@Override
	public void clock(long ticks)
	{
		mi.write32bit(MI_INTR_REG, MI_INTR_SET_VI);
		timer.write32bit(3, refreshScreen());
	}

	@Override
	public int read32bit(int reg)
	{
		switch ((reg - 0x04400000) >> 2)
		{
		case 0:
			return regVI[VI_STATUS_REG];
		case 1:
			return regVI[VI_ORIGIN_REG];
		case 2:
			return regVI[VI_WIDTH_REG];
		case 3:
			return regVI[VI_INTR_REG];
		case 4:
			return regVI[VI_CURRENT_REG];
		case 5:
			return regVI[VI_BURST_REG];
		case 6:
			return regVI[VI_V_SYNC_REG];
		case 7:
			return regVI[VI_H_SYNC_REG];
		case 8:
			return regVI[VI_LEAP_REG];
		case 9:
			return regVI[VI_H_START_REG];
		case 10:
			return regVI[VI_V_START_REG];
		case 11:
			return regVI[VI_V_BURST_REG];
		case 12:
			return regVI[VI_X_SCALE_REG];
		case 13:
			return regVI[VI_Y_SCALE_REG];
		case 16:
			return frameLimit ? 1 : 0;
		case 17:
			return useFrameBuffer ? 1 : 0;
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch ((reg - 0x04400000) >> 2)
		{
		case 0:
			if (regVI[VI_STATUS_REG] != value)
			{
				regVI[VI_STATUS_REG] = value;
			}
			break;
		case 1:
			regVI[VI_ORIGIN_REG] = (value & 0xFFFFFF);
			break;
		case 2:
			if (regVI[VI_WIDTH_REG] != value)
			{
				regVI[VI_WIDTH_REG] = value;
				dac.write32bit(DAC_WIDTH_BUF_REG, value);
				drawSurface.setPreferredSize(new Dimension(regVI[VI_WIDTH_REG], (regVI[VI_WIDTH_REG] >> 2) * 3));
				fbLenBytes = (3 * regVI[VI_WIDTH_REG] * regVI[VI_WIDTH_REG]) >> 1;
			}
			break;
		case 3:
			regVI[VI_INTR_REG] = value;
			break;
		case 4:
			mi.write32bit(MI_INTR_REG, MI_INTR_CLR_VI);
			break;
		case 5:
			regVI[VI_BURST_REG] = value;
			break;
		case 6:
			regVI[VI_V_SYNC_REG] = value;
			break;
		case 7:
			regVI[VI_H_SYNC_REG] = value;
			break;
		case 8:
			regVI[VI_LEAP_REG] = value;
			break;
		case 9:
			regVI[VI_H_START_REG] = value;
			break;
		case 10:
			regVI[VI_V_START_REG] = value;
			break;
		case 11:
			regVI[VI_V_BURST_REG] = value;
			break;
		case 12:
			regVI[VI_X_SCALE_REG] = value;
			break;
		case 13:
			regVI[VI_Y_SCALE_REG] = value;
			break;
		case 14:
			break;
		case 16:
			frameLimit = value != 0;
			break;
		case 17:
			useFrameBuffer = value != 0;
			break;
		}
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("framelimit"))
			return frameLimit;
		else if (key.equals("framebuffer")) return useFrameBuffer;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("framelimit"))
			frameLimit = (Boolean) value;
		else if (key.equals("framebuffer"))
			useFrameBuffer = (Boolean) value;
		else if (key.equals("screen"))
		{
			drawSurface = (Component) value;
			if (dac != null)
			{
				((Configurable) dac).writeConfig("screen", value);
			}
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private int refreshScreen()
	{
		if (oldViVsyncReg != regVI[VI_V_SYNC_REG])
		{
			if (regVI[VI_V_SYNC_REG] == 0)
			{
				viIntrTime = 500000;
			}
			else
			{
				viIntrTime = (regVI[VI_V_SYNC_REG] + 1) * 1500;
				if ((regVI[VI_V_SYNC_REG] % 1) != 0)
				{
					viIntrTime -= 38;
				}
			}
		}
		if ((regVI[VI_STATUS_REG] & 0x10) != 0)
		{
			if (viFieldNumber == 0)
			{
				viFieldNumber = 1;
			}
			else
			{
				viFieldNumber = 0;
			}
		}
		else
		{
			viFieldNumber = 0;
		}

		if ((currentFrame & 7) == 0)
		{
			long frametime = System.currentTimeMillis();
			frames[(currentFrame >> 3) % NUM_FRAMES] = frametime - lastFrame;
			lastFrame = frametime;
			if (currentFrame > (NUM_FRAMES << 3))
			{
				float total = 0.0f;
				for (int count = 0; count < NUM_FRAMES; count++)
					total += frames[count];
				dac.write32bit(DAC_FPS_REG, (int) (frequency / (total / (NUM_FRAMES << 3))));
			}
			else
			{
				dac.write32bit(DAC_FPS_REG, 0);
			}
		}
		currentFrame += 1;

		if (useFrameBuffer)
		{
			dac.write32bit(8, regVI[VI_ORIGIN_REG] & 0x00FFFFFF);
			dac.write32bit(9, fbLenBytes);
		}
		else
		{
			// dp.swapbuffers
			mi.read32bit(0x04100034);
		}

		currentTime = System.currentTimeMillis();
		if (frameLimit)
		{
			next_game_tick += SKIP_TICKS;
			sleep_time = next_game_tick - currentTime;
			if (sleep_time >= 0)
			{
				try
				{
					Thread.sleep(sleep_time);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		next_game_tick = System.currentTimeMillis();

		return viIntrTime;
	}
}
