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

package jario.n64.cartridge;

import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class FlashRam implements Hardware, Bus32bit, BusDMA, Configurable
{
	private static final String AUTO_SAVE_DIR = "./save/default.fla";

	private static final int FLASHRAM_MODE_NOPES = 0;
	private static final int FLASHRAM_MODE_ERASE = 1;
	private static final int FLASHRAM_MODE_WRITE = 2;
	private static final int FLASHRAM_MODE_READ = 3;
	private static final int FLASHRAM_MODE_STATUS = 4;

	private int flashRamOffset;
	private int flashFlag = FLASHRAM_MODE_NOPES;
	private RandomAccessFile hFlashRamFile;
	private File file;
	private byte[] flashRamPointer;
	private int flashRamPointerOffset;
	private long flashStatus = 0;

	public FlashRam()
	{
		file = new File(AUTO_SAVE_DIR);
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void reset()
	{
		close();
	}

	@Override
	public int read32bit(int reg)
	{
		switch (reg)
		{
		case 0:
			return (int) (flashStatus >> 32);
		default:
			System.err.printf("Reading from flash ram status (%X)\n", reg);
			return (int) (flashStatus >> 32);
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		if (reg != 0x4000) { return; }
		byte[] emptyBlock = new byte[128];
		switch (value & 0xFF000000)
		{
		case 0xD2000000:
			switch (flashFlag)
			{
			case FLASHRAM_MODE_NOPES:
				break;
			case FLASHRAM_MODE_READ:
				break;
			case FLASHRAM_MODE_STATUS:
				break;
			case FLASHRAM_MODE_ERASE:
				Arrays.fill(emptyBlock, (byte) 0xFF);
				if (hFlashRamFile == null)
				{
					if (!loadFlashram()) { return; }
				}
				try
				{
					hFlashRamFile.seek(flashRamOffset);
					hFlashRamFile.write(emptyBlock, 0, 128);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				break;
			case FLASHRAM_MODE_WRITE:
				if (hFlashRamFile == null)
				{
					if (!loadFlashram()) { return; }
				}
				try
				{
					hFlashRamFile.seek(flashRamOffset);
					hFlashRamFile.write(flashRamPointer, flashRamPointerOffset, 128);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				break;
			default:
				System.err.printf("Writing %X to flash ram command register\nFlashFlag: %d\n", value, flashFlag);
			}
			flashFlag = FLASHRAM_MODE_NOPES;
			break;
		case 0xE1000000:
			flashFlag = FLASHRAM_MODE_STATUS;
			flashStatus = 0x1111800100C20000L;
			break;
		case 0xF0000000:
			flashFlag = FLASHRAM_MODE_READ;
			flashStatus = 0x11118004F0000000L;
			break;
		case 0x4B000000:
			flashRamOffset = (value & 0xffff) * 128;
			break;
		case 0x78000000:
			flashFlag = FLASHRAM_MODE_ERASE;
			flashStatus = 0x1111800800C20000L;
			break;
		case 0xB4000000:
			flashFlag = FLASHRAM_MODE_WRITE; // ????
			break;
		case 0xA5000000:
			flashRamOffset = (value & 0xffff) * 128;
			flashStatus = 0x1111800400C20000L;
			break;
		default:
			System.err.printf("Writing %X to flash ram command register\n", value);
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
		if (key.equals("file")) file = new File(value.toString());
	}

	@Override
	public void readDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		switch (flashFlag)
		{
		case FLASHRAM_MODE_READ:
			if (hFlashRamFile == null)
			{
				if (!loadFlashram())
					return;
			}
			if (length > 0x10000)
			{
				System.err.printf("DmaFromFlashram FlipBuffer to small (len: %d)\n", length);
				length = 0x10000;
			}
			if ((length & 3) != 0)
			{
				System.err.printf("Unaligned flash ram read ???\n");
				return;
			}
			pAddr = pAddr << 1;
			try
			{
				hFlashRamFile.seek(pAddr);
				hFlashRamFile.read(dma.array(), offset, length);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			break;
		case FLASHRAM_MODE_STATUS:
			if (pAddr != 0 && length != 8)
				System.err.printf("Reading flashstatus not being handled correctly\nStart: %X len: %X\n", pAddr, length);
			ByteBuffer dest = ByteBuffer.wrap(dma.array());
			dest.putInt(offset + 0, (int) (flashStatus >> 32));
			dest.putInt(offset + 4, (int) (flashStatus));
			break;
		default:
			System.err.printf("DmaFromFlashram Start: %X, Offset: %X len: %X\n", offset, pAddr, length);
		}
	}

	@Override
	public void writeDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		switch (flashFlag)
		{
		case FLASHRAM_MODE_WRITE:
			flashRamPointer = dma.array();
			flashRamPointerOffset = offset;
			break;
		default:
			System.err.printf("DmaToFlashram Start: %X, Offset: %X len: %X\n", offset, pAddr, length);
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void close()
	{
		if (hFlashRamFile != null)
		{
			try
			{
				hFlashRamFile.close();
				hFlashRamFile = null;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private boolean loadFlashram()
	{
		try
		{
			hFlashRamFile = new RandomAccessFile(file, "rwd");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
