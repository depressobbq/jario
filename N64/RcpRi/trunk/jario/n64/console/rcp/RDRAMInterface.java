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

public class RDRAMInterface implements Hardware, Bus32bit
{
	private static final int RI_MODE_REG = 0;
	private static final int RI_CONFIG_REG = 1;
	private static final int RI_CURRENT_LOAD_REG = 2;
	private static final int RI_SELECT_REG = 3;
	private static final int RI_REFRESH_REG = 4;
	private static final int RI_LATENCY_REG = 5;
	private static final int RI_RERROR_REG = 6;
	private static final int RI_WERROR_REG = 7;

	private int[] regRI = new int[8];

	public RDRAMInterface()
	{
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0: // rdram
			break;
		case 1: // mips
			break;
		case 2: // unused
			break;
		case 3: // timing
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
		switch ((reg - 0x04700000) >> 2)
		{
		case 0:
			return regRI[RI_MODE_REG];
		case 1:
			return regRI[RI_CONFIG_REG];
		case 2:
			return regRI[RI_CURRENT_LOAD_REG];
		case 3:
			return regRI[RI_SELECT_REG];
		case 4:
			return regRI[RI_REFRESH_REG];
		case 5:
			return regRI[RI_LATENCY_REG];
		case 6:
			return regRI[RI_RERROR_REG];
		case 7:
			return regRI[RI_WERROR_REG];
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch ((reg - 0x04700000) >> 2)
		{
		case 0:
			regRI[RI_MODE_REG] = value;
			break;
		case 1:
			regRI[RI_CONFIG_REG] = value;
			break;
		case 2:
			regRI[RI_CURRENT_LOAD_REG] = value;
			break;
		case 3:
			regRI[RI_SELECT_REG] = value;
			break;
		case 4:
			regRI[RI_REFRESH_REG] = value;
			break;
		case 5:
			regRI[RI_LATENCY_REG] = value;
			break;
		case 6:
			regRI[RI_RERROR_REG] = value;
			break;
		case 7:
			regRI[RI_WERROR_REG] = value;
			break;
		}
	}
}
