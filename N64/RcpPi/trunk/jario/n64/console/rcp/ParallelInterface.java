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

import java.nio.ByteBuffer;

import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

public class ParallelInterface implements Hardware, Clockable, Bus32bit
{
	private static final int MI_INTR_REG = 0x04300008;

	private static final int MI_INTR_CLR_PI = 0x0100; /* Bit 8: clear PI interrupt */
	private static final int MI_INTR_SET_PI = 0x0200; /* Bit 9: set PI interrupt */
	// private static final int MI_INTR_MASK_CLR_PI = 0x0100; /* Bit 8: clear PI mask */
	// private static final int MI_INTR_MASK_SET_PI = 0x0200; /* Bit 9: set PI mask */
	// private static final int MI_INTR_MASK_PI = 0x10; /* Bit 4: PI intr mask */
	// private static final int MI_INTR_PI = 0x10; /* Bit 4: PI intr */

	// Parallel Interface (PI) Registers
	private static final int PI_DRAM_ADDR_REG = 0;
	private static final int PI_CART_ADDR_REG = 1;
	private static final int PI_RD_LEN_REG = 2;
	private static final int PI_WR_LEN_REG = 3;
	private static final int PI_STATUS_REG = 4;
	// private static final int PI_BSD_DOM1_LAT_REG = 5;
	private static final int PI_DOMAIN1_REG = 5;
	private static final int PI_BSD_DOM1_PWD_REG = 6;
	private static final int PI_BSD_DOM1_PGS_REG = 7;
	private static final int PI_BSD_DOM1_RLS_REG = 8;
	// private static final int PI_BSD_DOM2_LAT_REG = 9;
	private static final int PI_DOMAIN2_REG = 9;
	private static final int PI_BSD_DOM2_PWD_REG = 10;
	private static final int PI_BSD_DOM2_PGS_REG = 11;
	private static final int PI_BSD_DOM2_RLS_REG = 12;

	private static final int RDRAM_CAPACITY_REG = 0x03F00028;

	// private static final int CART_CIC_REG = 1;

	private static final int PI_STATUS_DMA_BUSY = 0x01;
	// private static final int PI_STATUS_IO_BUSY = 0x02;
	// private static final int PI_STATUS_ERROR = 0x04;

	// private static final int PI_SET_RESET = 0x01;
	private static final int PI_CLR_INTR = 0x02;

	private int[] regPI = new int[13];
	private boolean dmaUsed;

	private Bus32bit rdram;
	private BusDMA rdramDMA;
	private Bus32bit mi;
	private BusDMA rom;
	private Bus32bit timer;

	public ParallelInterface()
	{
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			rdram = (Bus32bit) bus;
			rdramDMA = (BusDMA) bus;
			break;
		case 1:
			mi = (Bus32bit) bus;
			break;
		case 2:
			rom = (BusDMA) bus;
			break;
		case 3:
			timer = (Bus32bit) bus;
			break;
		default:
			System.err.println("Attempting to connect bus on invalid port: " + port);
			break;
		}
	}

	@Override
	public void reset()
	{
		dmaUsed = false;
	}

	@Override
	public void clock(long ticks)
	{
		regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
		mi.write32bit(MI_INTR_REG, MI_INTR_SET_PI);
		timer.write32bit(2, -(int) ticks);
	}

	@Override
	public int read32bit(int reg)
	{
		switch ((reg - 0x04600000) >> 2)
		{
		case 4:
			return regPI[PI_STATUS_REG];
		case 5:
			return regPI[PI_DOMAIN1_REG];
		case 6:
			return regPI[PI_BSD_DOM1_PWD_REG];
		case 7:
			return regPI[PI_BSD_DOM1_PGS_REG];
		case 8:
			return regPI[PI_BSD_DOM1_RLS_REG];
		case 9:
			return regPI[PI_DOMAIN2_REG];
		case 10:
			return regPI[PI_BSD_DOM2_PWD_REG];
		case 11:
			return regPI[PI_BSD_DOM2_PGS_REG];
		case 12:
			return regPI[PI_BSD_DOM2_RLS_REG];
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch ((reg - 0x04600000) >> 2)
		{
		case 0:
			regPI[PI_DRAM_ADDR_REG] = value;
			break;
		case 1:
			regPI[PI_CART_ADDR_REG] = value;
			break;
		case 2:
			regPI[PI_RD_LEN_REG] = value;
			piDmaRead(regPI[PI_DRAM_ADDR_REG], regPI[PI_CART_ADDR_REG], regPI[PI_RD_LEN_REG]);
			regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
			mi.write32bit(MI_INTR_REG, MI_INTR_SET_PI);
			break;
		case 3:
			regPI[PI_WR_LEN_REG] = value;
			regPI[PI_STATUS_REG] |= PI_STATUS_DMA_BUSY;
			piDmaWrite(regPI[PI_DRAM_ADDR_REG], regPI[PI_CART_ADDR_REG], regPI[PI_WR_LEN_REG]);
			regPI[PI_STATUS_REG] &= ~PI_STATUS_DMA_BUSY;
			mi.write32bit(MI_INTR_REG, MI_INTR_SET_PI);
			break;
		case 4:
			if ((value & PI_CLR_INTR) != 0)
			{
				mi.write32bit(MI_INTR_REG, MI_INTR_CLR_PI);
			}
			break;
		case 5:
			regPI[PI_DOMAIN1_REG] = (value & 0xFF);
			break;
		case 6:
			regPI[PI_BSD_DOM1_PWD_REG] = (value & 0xFF);
			break;
		case 7:
			regPI[PI_BSD_DOM1_PGS_REG] = (value & 0xFF);
			break;
		case 8:
			regPI[PI_BSD_DOM1_RLS_REG] = (value & 0xFF);
			break;
		case 9:
			regPI[PI_DOMAIN2_REG] = (value & 0xFF);
			break;
		case 10:
			regPI[PI_BSD_DOM2_PWD_REG] = (value & 0xFF);
			break;
		case 11:
			regPI[PI_BSD_DOM2_PGS_REG] = (value & 0xFF);
			break;
		case 12:
			regPI[PI_BSD_DOM2_RLS_REG] = (value & 0xFF);
			break;
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void piDmaRead(int dramAddr, int cartAddr, int rdLen)
	{
		if (dramAddr + rdLen + 1 > rdram.read32bit(RDRAM_CAPACITY_REG))
		{
			System.err.printf("PI_DMA_READ not in Memory\n");
			return;
		}

		ByteBuffer tmp = ByteBuffer.allocate(rdLen + 1);
		rdramDMA.readDMA(dramAddr, tmp, 0, rdLen + 1);
		rom.writeDMA(cartAddr - 0x05000000, tmp, 0, rdLen + 1);
	}

	private void piDmaWrite(int dramAddr, int cartAddr, int wrLen)
	{
		if (dramAddr + wrLen + 1 > rdram.read32bit(RDRAM_CAPACITY_REG))
		{
			System.err.printf("PI_DMA_WRITE not in Memory\n");
			return;
		}

		ByteBuffer tmp = ByteBuffer.allocate(wrLen + 1);
		rom.readDMA(cartAddr - 0x05000000, tmp, 0, wrLen + 1);

		if (cartAddr >= 0x10000000 && cartAddr <= 0x1FBFFFFF)
		{
			if (!dmaUsed)
			{
				dmaUsed = true;
				switch ((Integer) ((Configurable) rom).readConfig("cic"))
				{
				case 1:
					rdram.write32bit(0x318, rdram.read32bit(RDRAM_CAPACITY_REG));
					break;
				case 2:
					rdram.write32bit(0x318, rdram.read32bit(RDRAM_CAPACITY_REG));
					break;
				case 3:
					rdram.write32bit(0x318, rdram.read32bit(RDRAM_CAPACITY_REG));
					break;
				case 5:
					rdram.write32bit(0x3F0, rdram.read32bit(RDRAM_CAPACITY_REG));
					break;
				case 6:
					rdram.write32bit(0x318, rdram.read32bit(RDRAM_CAPACITY_REG));
					break;
				default:
					System.err.printf("Unhandled CicChip(%d) in first DMA\n", (Integer) ((Configurable) rom).readConfig("cic"));
				}
			}
		}

		rdramDMA.writeDMA(dramAddr, tmp, 0, wrLen + 1);
	}
}
