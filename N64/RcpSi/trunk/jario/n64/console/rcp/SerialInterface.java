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

import jario.hardware.Bus1bit;
import jario.hardware.Bus32bit;
import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Hardware;

public class SerialInterface implements Hardware, Clockable, Bus32bit
{
	private static final int MI_INTR_REG = 0x04300008;

	private static final int MI_INTR_CLR_SI = 0x0004; /* Bit 2: clear SI interrupt */
	private static final int MI_INTR_SET_SI = 0x0008; /* Bit 3: set SI interrupt */
	// private static final int MI_INTR_MASK_CLR_SI = 0x0004; /* Bit 2: clear SI mask */
	// private static final int MI_INTR_MASK_SET_SI = 0x0008; /* Bit 3: set SI mask */
	// private static final int MI_INTR_MASK_SI = 0x02; /* Bit 1: SI intr mask */
	// private static final int MI_INTR_SI = 0x02; /* Bit 1: SI intr */

	private static final int SI_DRAM_ADDR_REG = 0;
	private static final int SI_PIF_ADDR_RD64B_REG = 1;
	private static final int SI_PIF_ADDR_WR64B_REG = 2;
	private static final int SI_STATUS_REG = 3;

	// private static final int SI_STATUS_DMA_BUSY = 0x0001;
	// private static final int SI_STATUS_RD_BUSY = 0x0002;
	// private static final int SI_STATUS_DMA_ERROR = 0x0008;
	private static final int SI_STATUS_INTERRUPT = 0x1000;

	private static final int RDRAM_CAPACITY_REG = 0x03F00028;

	private static final int PIF_READ_REG = 0;

	private Bus8bit rdram;
	private Bus32bit mi;
	private Bus8bit pif;
	private Bus32bit timer;

	private int[] regSI = new int[4];

	public SerialInterface()
	{
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			rdram = (Bus8bit) bus;
			break;
		case 1:
			mi = (Bus32bit) bus;
			break;
		case 2:
			pif = (Bus8bit) bus;
			break;
		case 3:
			timer = (Bus32bit) bus;
			break;
		}
	}

	@Override
	public void reset()
	{
	}

	@Override
	public void clock(long ticks)
	{
		regSI[SI_STATUS_REG] |= SI_STATUS_INTERRUPT;
		mi.write32bit(MI_INTR_REG, MI_INTR_SET_SI);
		timer.write32bit(1, -(int) ticks);
	}

	@Override
	public int read32bit(int reg)
	{
		switch ((reg - 0x04800000) >> 2)
		{
		case 6:
			return regSI[SI_STATUS_REG];
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch ((reg - 0x04800000) >> 2)
		{
		case 0:
			regSI[SI_DRAM_ADDR_REG] = value;
			break;
		case 1:
			regSI[SI_PIF_ADDR_RD64B_REG] = value;
			pifRamDmaRead(rdram, regSI[SI_DRAM_ADDR_REG]);
			regSI[SI_STATUS_REG] |= SI_STATUS_INTERRUPT;
			mi.write32bit(MI_INTR_REG, MI_INTR_SET_SI);
			break;
		case 4:
			regSI[SI_PIF_ADDR_WR64B_REG] = value;
			pifRamDmaWrite(rdram, regSI[SI_DRAM_ADDR_REG]);
			regSI[SI_STATUS_REG] |= SI_STATUS_INTERRUPT;
			mi.write32bit(MI_INTR_REG, MI_INTR_SET_SI);
			break;
		case 6:
			regSI[SI_STATUS_REG] &= ~SI_STATUS_INTERRUPT;
			mi.write32bit(MI_INTR_REG, MI_INTR_CLR_SI);
			break;
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void pifRamDmaRead(Bus8bit rdram, int dramAddr)
	{
		if (dramAddr > ((Bus32bit) rdram).read32bit(RDRAM_CAPACITY_REG))
		{
			System.err.printf("SI DMA READ\nSI_DRAM_ADDR_REG not in RDRam space\n");
			return;
		}

		dramAddr &= 0xFFFFFFF8;
		((Bus1bit) pif).write1bit(PIF_READ_REG, true);
		if (dramAddr < 0)
		{
			for (int count = 0; count < 64; count++, dramAddr++)
			{
				if (dramAddr < 0)
				{
					continue;
				}
				rdram.write8bit(dramAddr, pif.read8bit(count + 0x7C0));
			}
		}
		else
		{
			for (int i = 0; i < 64; i++, dramAddr++)
			{
				rdram.write8bit(dramAddr, pif.read8bit(i + 0x7C0));
			}
		}
	}

	private void pifRamDmaWrite(Bus8bit rdram, int dramAddr)
	{
		if (dramAddr > ((Bus32bit) rdram).read32bit(RDRAM_CAPACITY_REG))
		{
			System.err.printf("SI DMA WRITE\nSI_DRAM_ADDR_REG not in RDRam space\n");
			return;
		}

		dramAddr &= 0xFFFFFFF8;
		if (dramAddr < 0)
		{
			for (int count = 0; count < 64; count++, dramAddr++)
			{
				if (dramAddr < 0)
				{
					pif.write8bit(count + 0x7C0, (byte) 0);
					continue;
				}
				pif.write8bit(count + 0x7C0, rdram.read8bit(dramAddr));
			}
		}
		else
		{
			for (int i = 0; i < 64; i++, dramAddr++)
			{
				pif.write8bit(i + 0x7C0, rdram.read8bit(dramAddr));
			}
		}
		((Bus1bit) pif).write1bit(PIF_READ_REG, false);
	}
}
