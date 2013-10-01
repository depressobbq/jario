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

package jario.n64.console.rcp;

import jario.hardware.Bus32bit;
import jario.hardware.Clockable;
import jario.hardware.Hardware;

public class Timer implements Hardware, Clockable, Bus32bit
{
	private static final int MAX_TIMERS = 4;

	private int[] nextTimer = new int[MAX_TIMERS];
	private int currentTimerType;
	private int timer;
	private boolean[] active = new boolean[MAX_TIMERS];

	private Clockable[] devices = new Clockable[4];

	public Timer()
	{
		currentTimerType = -1;
		timer = 0;
		for (int count = 0; count < MAX_TIMERS; count++)
			active[count] = false;
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		if (port < 0 || port > 3)
			return;
		devices[port] = (Clockable) bus;
	}

	@Override
	public void reset()
	{
		timer = 0;
	}

	@Override
	public void clock(long ticks)
	{
		timer -= ticks;
		checkTimerDone();
	}

	@Override
	public int read32bit(int reg)
	{
		switch (reg)
		{
		case 4:
			return timer;
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch (reg)
		{
		case 0:
			changeTimer(0, timer + nextTimer[0] + value);
			break;
		case 1:
			changeTimer(1, timer + nextTimer[1] + value);
			break;
		case 2:
			changeTimer(2, timer + nextTimer[2] + value);
			break;
		case 3:
			changeTimer(3, timer + nextTimer[3] + value);
			break;
		case 4:
			timer = value;
			break;
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void checkTimerDone()
	{
		if (timer >= 0 || currentTimerType == -1)
			return;
		devices[currentTimerType].clock(timer);
	}

	private void changeTimer(int type, int value)
	{
		if (value == 0)
		{
			nextTimer[type] = 0;
			active[type] = false;
			return;
		}

		nextTimer[type] = value - timer;
		active[type] = true;

		for (int count = 1; count < MAX_TIMERS; count++)
		{
			if (!active[count])
				continue;
			nextTimer[count] += timer;
		}
		currentTimerType = -1;
		timer = 0x7FFFFFFF;
		for (int count = 1; count < MAX_TIMERS; count++)
		{
			if (!active[count])
				continue;
			if (nextTimer[count] >= timer)
				continue;
			timer = nextTimer[count];
			currentTimerType = count;
		}
		if (currentTimerType == -1)
		{
			System.err.printf("No active timers ???\nEmulation Stoped\n");
			System.exit(0);
		}
		for (int count = 1; count < MAX_TIMERS; count++)
		{
			if (!active[count])
				continue;
			nextTimer[count] -= timer;
		}
	}
}
