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

package jario.n64.console;

import jario.hardware.Bus16bit;
import jario.hardware.Bus32bit;
import jario.hardware.Bus64bit;
import jario.hardware.Bus8bit;
import jario.hardware.BusDMA;
import jario.hardware.Hardware;

import java.nio.ByteBuffer;

public class Rdram implements Hardware, Bus8bit, Bus16bit, Bus32bit, Bus64bit, BusDMA
{
	private static final int RDRAM_CONFIG_REG = 0;
	private static final int RDRAM_DEVICE_ID_REG = 1;
	private static final int RDRAM_DELAY_REG = 2;
	private static final int RDRAM_MODE_REG = 3;
	private static final int RDRAM_REF_INTERVAL_REG = 4;
	private static final int RDRAM_REF_ROW_REG = 5;
	private static final int RDRAM_RAS_INTERVAL_REG = 6;
	private static final int RDRAM_MIN_INTERVAL_REG = 7;
	private static final int RDRAM_ADDR_SELECT_REG = 8;
	private static final int RDRAM_DEVICE_MANUF_REG = 9;

	private byte[] rdram;

	private int[] regRDRAM = new int[10];

	public Rdram()
	{
		rdram = new byte[0x00800000]; // (8MB);
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
	public byte read8bit(int pAddr)
	{
		return rdram[pAddr];
	}

	@Override
	public short read16bit(int pAddr)
	{
		return (short) (((rdram[pAddr] & 0xFF) << 8) | (rdram[pAddr + 1] & 0xFF));
	}

	@Override
	public int read32bit(int pAddr)
	{
		if (pAddr < 0x00800000)
		{
			return (rdram[pAddr] << 24) | ((rdram[pAddr + 1] & 0xFF) << 16) | ((rdram[pAddr + 2] & 0xFF) << 8) | (rdram[pAddr + 3] & 0xFF);
		}
		else if ((pAddr & 0xFFF00000) == 0x03F00000)
		{
			switch ((pAddr - 0x03F00000) >> 2)
			{
			case 0:
				return regRDRAM[RDRAM_CONFIG_REG];
			case 1:
				return regRDRAM[RDRAM_DEVICE_ID_REG];
			case 2:
				return regRDRAM[RDRAM_DELAY_REG];
			case 3:
				return regRDRAM[RDRAM_MODE_REG];
			case 4:
				return regRDRAM[RDRAM_REF_INTERVAL_REG];
			case 5:
				return regRDRAM[RDRAM_REF_ROW_REG];
			case 6:
				return regRDRAM[RDRAM_RAS_INTERVAL_REG];
			case 7:
				return regRDRAM[RDRAM_MIN_INTERVAL_REG];
			case 8:
				return regRDRAM[RDRAM_ADDR_SELECT_REG];
			case 9:
				return regRDRAM[RDRAM_DEVICE_MANUF_REG];
			case 10:
				return rdram.length;
			default:
				return 0;
			}
		}
		else
		{
			System.err.println("Illegal Memory LW access: " + Integer.toHexString(pAddr));
			return 0;
		}
	}

	@Override
	public long read64bit(int pAddr)
	{
		return ((rdram[pAddr] & 0xFFL) << 56) | ((rdram[pAddr + 1] & 0xFFL) << 48) | ((rdram[pAddr + 2] & 0xFFL) << 40) | ((rdram[pAddr + 3] & 0xFFL) << 32)
				| ((rdram[pAddr + 4] & 0xFFL) << 24) | ((rdram[pAddr + 5] & 0xFFL) << 16) | ((rdram[pAddr + 6] & 0xFFL) << 8) | (rdram[pAddr + 7] & 0xFFL);
	}

	@Override
	public void readDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		System.arraycopy(rdram, pAddr, dma.array(), offset, length);
	}

	@Override
	public void write8bit(int pAddr, byte value)
	{
		rdram[pAddr] = value;
	}

	@Override
	public void write16bit(int pAddr, short value)
	{
		rdram[pAddr] = (byte) (value >> 8);
		rdram[pAddr + 1] = (byte) value;
	}

	@Override
	public void write32bit(int pAddr, int value)
	{
		if (pAddr < 0x00800000)
		{
			rdram[pAddr] = (byte) (value >> 24);
			rdram[pAddr + 1] = (byte) (value >> 16);
			rdram[pAddr + 2] = (byte) (value >> 8);
			rdram[pAddr + 3] = (byte) value;
		}
		else if ((pAddr & 0xFFF00000) == 0x03F00000)
		{
			switch ((pAddr - 0x03F00000) >> 2)
			{
			case 0:
				regRDRAM[RDRAM_CONFIG_REG] = value;
				break;
			case 1:
				regRDRAM[RDRAM_DEVICE_ID_REG] = value;
				break;
			case 2:
				regRDRAM[RDRAM_DELAY_REG] = value;
				break;
			case 3:
				regRDRAM[RDRAM_MODE_REG] = value;
				break;
			case 4:
				regRDRAM[RDRAM_REF_INTERVAL_REG] = value;
				break;
			case 5:
				regRDRAM[RDRAM_REF_ROW_REG] = value;
				break;
			case 6:
				regRDRAM[RDRAM_RAS_INTERVAL_REG] = value;
				break;
			case 7:
				regRDRAM[RDRAM_MIN_INTERVAL_REG] = value;
				break;
			case 8:
				regRDRAM[RDRAM_ADDR_SELECT_REG] = value;
				break;
			case 9:
				regRDRAM[RDRAM_DEVICE_MANUF_REG] = value;
				break;
			case 0x1001:
				break;
			case 0x2001:
				break;
			case 0x20001:
				break;
			case 0x20002:
				break;
			case 0x20003:
				break;
			case 0x20005:
				break;
			}
		}
		else
		{
			System.err.println("Illegal Memory LW access: " + Integer.toHexString(pAddr));
		}
	}

	@Override
	public void write64bit(int pAddr, long value)
	{
		rdram[pAddr] = (byte) (value >> 56);
		rdram[pAddr + 1] = (byte) (value >> 48);
		rdram[pAddr + 2] = (byte) (value >> 40);
		rdram[pAddr + 3] = (byte) (value >> 32);
		rdram[pAddr + 4] = (byte) (value >> 24);
		rdram[pAddr + 5] = (byte) (value >> 16);
		rdram[pAddr + 6] = (byte) (value >> 8);
		rdram[pAddr + 7] = (byte) value;
	}

	@Override
	public void writeDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		System.arraycopy(dma.array(), offset, rdram, pAddr, length);
	}
}
