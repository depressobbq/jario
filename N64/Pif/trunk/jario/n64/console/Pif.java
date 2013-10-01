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

import jario.hardware.Bus1bit;
import jario.hardware.Bus32bit;
import jario.hardware.Bus8bit;
import jario.hardware.BusDMA;
import jario.hardware.Hardware;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Pif implements Hardware, Bus1bit, Bus8bit, Bus32bit
{
	private static final int EEPROM_4K = 1;
	private static final int EEPROM_16K = 2;

	private BusDMA[] ctrbus;
	private BusDMA eeprom;
	private byte[] pifRom;
	private ByteBuffer pifRam;
	private boolean debug;

	private BusDMA noController = new BusDMA()
	{
		@Override
		public void readDMA(int pAddr, ByteBuffer dma, int offset, int length)
		{
			switch (dma.array()[offset + 2] & 0xFF)
			{
			case 0x01: // read controller
				break;
			case 0x02: // read from controller pack
				break;
			case 0x03: // write controller pak
				break;
			}
		}

		@Override
		public void writeDMA(int pAddr, ByteBuffer dmaObj, int offset, int length)
		{
			byte[] dma = dmaObj.array();
			switch (dma[offset + 2] & 0xFF)
			{
			case 0x00: // check
			case 0xFF: // reset & check ?
				if ((dma[offset + 1] & 0x80) != 0)
					break;
				dma[offset + 1] = (byte) ((dma[offset + 1] & 0xFF) | 0x80);
				break;
			case 0x01: // read controller
				dma[offset + 1] = (byte) ((dma[offset + 1] & 0xFF) | 0x80);
				break;
			case 0x02: // read from controller pack
				dma[offset + 1] = (byte) ((dma[offset + 1] & 0xFF) | 0x80);
				break;
			case 0x03: // write controller pak
				dma[offset + 1] = (byte) ((dma[offset + 1] & 0xFF) | 0x80);
				break;
			default:
				if (debug)
					System.err.printf("Unknown ControllerCommand %d\n", dma[offset + 2]);
			}
		}
	};

	public Pif()
	{
		pifRom = new byte[0x7C0];
		pifRam = ByteBuffer.wrap(new byte[0x40]);
		ctrbus = new BusDMA[4];
		for (int i = 0; i < 4; i++)
			ctrbus[i] = noController;
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			ctrbus[0] = (bus != null) ? (BusDMA) bus : noController;
			break;
		case 1:
			ctrbus[1] = (bus != null) ? (BusDMA) bus : noController;
			break;
		case 2:
			ctrbus[2] = (bus != null) ? (BusDMA) bus : noController;
			break;
		case 3:
			ctrbus[3] = (bus != null) ? (BusDMA) bus : noController;
			break;
		case 4:
			eeprom = (BusDMA) bus;
			break;
		}
	}

	@Override
	public void reset()
	{
		pifRom = new byte[0x7C0];
		pifRam = ByteBuffer.wrap(new byte[0x40]);
	}

	@Override
	public boolean read1bit(int reg)
	{
		return false;
	}

	@Override
	public void write1bit(int reg, boolean value)
	{
		switch (reg)
		{
		case 0:
			if (value)
				pifRamRead();
			else
			{
				if ((pifRam.get(0x3F) & 0xFF) > 0x1)
					pifRamControl(pifRam.get(0x3F) & 0xFF);
				else
					pifRamWrite();
			}
			break;
		case 256:
			debug = value;
			break;
		}
	}

	@Override
	public byte read8bit(int pAddr)
	{
		if (pAddr < 0x7C0)
		{
			return pifRom[pAddr];
		}
		else if (pAddr < 0x800)
		{
			return pifRam.get(pAddr - 0x7C0);
		}
		else
		{
			System.err.println("Illegal PifRam LW: " + Integer.toHexString(pAddr));
			return 0;
		}
	}

	@Override
	public int read32bit(int pAddr)
	{
		if (pAddr < 0x7C0)
		{
			return (pifRom[pAddr] << 24) | (pifRom[pAddr + 1] << 16) | (pifRom[pAddr + 2] << 8) | (pifRom[pAddr + 3]);
		}
		else if (pAddr < 0x800)
		{
			return pifRam.getInt(pAddr - 0x7C0);
		}
		else
		{
			System.err.println("Illegal PifRam LW: " + Integer.toHexString(pAddr));
			return 0;
		}
	}

	@Override
	public void write8bit(int pAddr, byte value)
	{
		if (pAddr < 0x7C0)
		{ // PIF Boot rom
			System.err.println("Illegal PifRam SW: " + Integer.toHexString(pAddr));
			return;
		}
		else if (pAddr < 0x800)
		{ // PIF RAM
			pifRam.put(pAddr - 0x7C0, value);
			if (pAddr == 0x7FC)
			{
				if ((pifRam.get(0x3F) & 0xFF) > 0x1)
					pifRamControl(pifRam.get(0x3F) & 0xFF);
				else
					pifRamWrite();
			}
			return;
		}
		System.err.println("Illegal PifRam Register SW: " + Integer.toHexString(pAddr));
		return;
	}

	@Override
	public void write32bit(int pAddr, int value)
	{
		if (pAddr < 0x7C0)
		{ // PIF Boot rom
			System.err.println("Illegal PifRam SW: " + Integer.toHexString(pAddr));
			return;
		}
		else if (pAddr < 0x800)
		{ // PIF RAM
			pifRam.putInt(pAddr - 0x7C0, value);
			if (pAddr == 0x7FC)
			{
				if ((pifRam.get(0x3F) & 0xFF) > 0x1)
					pifRamControl(pifRam.get(0x3F) & 0xFF);
				else
					pifRamWrite();
			}
			return;
		}
		System.err.println("Illegal PifRam Register SW: " + Integer.toHexString(pAddr));
		return;
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void pifRamRead()
	{
		int channel = 0;
		int curPos = 0;

		do
		{
			switch (pifRam.get(curPos) & 0xFF)
			{
			case 0x00:
				channel += 1;
				if (channel > 6)
				{
					curPos = 0x40;
				}
				break;
			case 0xFE:
				curPos = 0x40;
				break;
			case 0xFF:
				break;
			case 0xB4:
			case 0x56:
			case 0xB8:
				break; /* ??? */
			default:
				if ((pifRam.get(curPos) & 0xC0) == 0)
				{
					if (channel < 4)
					{
						ctrbus[channel].readDMA(channel, pifRam, curPos, 0);
					}
					curPos += (pifRam.get(curPos) & 0xFF) + (pifRam.get(curPos + 1) & 0x3F) + 1;
					channel += 1;
				}
				else
				{
					if (debug)
						System.err.printf("Unknown Command in PifRamRead(%X)\n", pifRam.get(curPos));
					curPos = 0x40;
				}
				break;
			}
			curPos += 1;
		} while (curPos < 0x40);
	}

	private void pifRamWrite()
	{
		int channel = 0;

		for (int curPos = 0; curPos < 0x40; curPos++)
		{
			switch (pifRam.get(curPos) & 0xFF)
			{
			case 0x00:
				channel += 1;
				if (channel > 6)
				{
					curPos = 0x40;
				}
				break;
			case 0xFE:
				curPos = 0x40;
				break;
			case 0xFF:
				break;
			case 0xB4:
			case 0x56:
			case 0xB8:
				break; /* ??? */
			default:
				if ((pifRam.get(curPos) & 0xC0) == 0)
				{
					if (channel < 4)
					{
						ctrbus[channel].writeDMA(channel, pifRam, curPos, 0);
					}
					else if (channel == 4)
					{
						pifRam.position(curPos);
						eepromCommand(pifRam.slice(), curPos, EEPROM_4K, debug);
						pifRam.position(0);
					}
					else
					{
						System.err.printf("Command on channel 5?\n");
					}
					curPos += (pifRam.get(curPos) & 0xFF) + (pifRam.get(curPos + 1) & 0x3F) + 1;
					channel += 1;
				}
				else
				{
					if (debug)
						System.err.printf("Unknown Command in PifRamWrite(%X)\n", pifRam.get(curPos));
					curPos = 0x40;
				}
				break;
			}
		}
		pifRam.put(0x3F, (byte) 0);
	}

	private void pifRamControl(int command)
	{
		switch (command)
		{
		case 0x08:
			pifRam.put(0x3F, (byte) 0);
			// serial interface (si) interrupt here ??
			break;
		case 0x10:
			// Arrays.fill(pifRom, 0, 0x7C0, (byte)0);
			break;
		case 0x30:
			pifRam.put(0x3F, (byte) 0x80);
			break;
		case 0xC0:
			Arrays.fill(pifRam.array(), 0, 0x40, (byte) 0);
			break;
		default:
			if (debug)
				System.err.printf("Unkown PifRam control: %d\n", pifRam.get(0x3F));
		}
	}

	private void eepromCommand(ByteBuffer command, int offset, int saveType, boolean showPifRamErrors)
	{
		switch (command.get(2))
		{
		case 0: // check
			if (saveType != EEPROM_4K && saveType != EEPROM_16K)
			{
				command.put(1, (byte) ((command.get(1) & 0xFF) | 0x80));
				break;
			}
			if (command.get(1) != 3)
			{
				command.put(1, (byte) ((command.get(1) & 0xFF) | 0x40));
				if ((command.get(1) & 3) > 0)
				{
					command.put(3, (byte) 0x00);
				}
				if (saveType == EEPROM_4K)
				{
					if ((command.get(1) & 3) > 1)
					{
						command.put(4, (byte) 0x80);
					}
				}
				else
				{
					if ((command.get(1) & 3) > 1)
					{
						command.put(4, (byte) 0xC0);
					}
				}
				if ((command.get(1) & 3) > 2)
				{
					command.put(5, (byte) 0x00);
				}
			}
			else
			{
				command.put(3, (byte) 0x00);
				command.put(4, (byte) ((saveType == EEPROM_4K) ? 0x80 : 0xC0));
				command.put(5, (byte) 0x00);
			}
			break;
		case 4: // Read from Eeprom
			if (command.get(0) != 2)
				System.err.printf("What am I meant to do with this Eeprom Command\n");
			if (command.get(1) != 8)
				System.err.printf("What am I meant to do with this Eeprom Command\n");
			eeprom.readDMA(command.get(3), command, offset + 4, 8);
			break;
		case 5:
			if (command.get(0) != 10)
				System.err.printf("What am I meant to do with this Eeprom Command\n");
			if (command.get(1) != 1)
				System.err.printf("What am I meant to do with this Eeprom Command\n");
			eeprom.writeDMA(command.get(3), command, offset + 4, 8);
			break;
		default:
			if (showPifRamErrors)
				System.err.printf("Unkown EepromCommand %d\n", command.get(2));
		}
	}
}
