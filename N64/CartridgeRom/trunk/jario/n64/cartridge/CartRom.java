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
import jario.hardware.Bus8bit;
import jario.hardware.BusDMA;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CartRom implements Hardware, Bus8bit, Bus32bit, BusDMA, Configurable
{
	private boolean debug;

	private static final int BUFFER = 2048;

	private Hardware sram;
	private Bus32bit flashRam;

	private ByteBuffer rom;
	private boolean writtenToRom;
	private int wroteToRom;
	private boolean flashRamUsed;

	public CartRom()
	{
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			sram = bus;
			break;
		case 1:
			flashRam = (Bus32bit) bus;
			break;
		}
	}

	@Override
	public void reset()
	{
		if (sram != null)
		{
			sram.reset();
		}
		if (flashRam != null)
		{
			((Hardware) flashRam).reset();
		}
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("cic")) return (rom == null ? 0 : getCicChipID());
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("romfile")) rom = loadDataFromRomFile((String) value);
		else if (key.equals("debug")) debug = (Boolean) value;
	}

	@Override
	public byte read8bit(int pAddr)
	{
		if (rom == null)
		{
			System.err.println("ROM loadByte: " + Integer.toHexString(pAddr));
			return 0;
		}

		if (pAddr < 0x1000000)
		{
			// Cartridge Domain 2 Address 1
			System.err.println("ROM loadByte Domain 2 Address 1: " + Integer.toHexString(pAddr));
			return 0;
		}
		else if (pAddr < 0x3000000)
		{
			// Cartridge Domain 1 Address 1
			System.err.println("ROM loadByte Domain 1 Address 1: " + Integer.toHexString(pAddr));
			return 0;
		}
		else if (pAddr < 0xB000000)
		{
			// Cartridge Domain 2 Address 2
			System.err.println("ROM loadByte Domain 2 Address 2: " + Integer.toHexString(pAddr));
			return 0;
		}
		else if (pAddr < 0x11000000)
		{
			// Cartridge Domain 1 Address 2
			if (writtenToRom)
			{
				System.err.println("Illegal LB: writtenToRom");
				return 0;
			}
			if ((pAddr - 0xB000000) < rom.capacity())
			{
				return rom.get(pAddr - 0xB000000);
			}
			else
			{
				return 0;
			}
		}
		System.err.println("ROM loadByte: " + Integer.toHexString(pAddr));
		return 0;
	}

	@Override
	public int read32bit(int pAddr)
	{
		if (rom == null)
		{
			System.err.println("ROM loadWord: " + Integer.toHexString(pAddr));
			return 0;
		}

		if (pAddr < 0x1000000)
		{
			// Cartridge Domain 2 Address 1
			return ((pAddr & 0xFFFF) << 16) | (pAddr & 0xFFFF);
		}
		else if (pAddr < 0x3000000)
		{
			// Cartridge Domain 1 Address 1
			System.err.println("ROM loadWord Domain 1 Address 1: " + Integer.toHexString(pAddr));
			return 0;
		}
		else if (pAddr < 0xB000000)
		{
			// Cartridge Domain 2 Address 2
			flashRamUsed = true;
			return flashRam.read32bit((pAddr - 0x3000000) >> 2);
		}
		else if (pAddr < 0x11000000)
		{
			// Cartridge Domain 1 Address 2
			if (writtenToRom)
			{
				writtenToRom = false;
				return wroteToRom;
			}
			if ((pAddr - 0xB000000) < rom.capacity())
			{
				return rom.getInt(pAddr - 0xB000000);
			}
			else
			{
				return ((pAddr & 0xFFFF) << 16) | (pAddr & 0xFFFF);
			}
		}
		System.err.println("ROM loadWord: " + Integer.toHexString(pAddr));
		return 0;
	}

	@Override
	public void readDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		if (rom == null)
		{
			System.err.println("ROM loadByteDma: " + Integer.toHexString(pAddr));
			return;
		}

		if (pAddr >= 0x03000000 && pAddr <= 0x03010000)
		{
			if (flashRamUsed)
			{
				((BusDMA) flashRam).readDMA(pAddr - 0x03000000, dma, offset, length);
			}
			else
			{
				((BusDMA) sram).readDMA(pAddr - 0x03000000, dma, offset, length);
			}
			return;
		}

		if (pAddr >= 0xB000000 && pAddr <= 0x1ABFFFFF)
		{
			if ((pAddr - 0xB000000) + length < rom.capacity())
			{
				for (int i = 0; i < length; i++)
				{
					dma.array()[offset + i] = rom.get(pAddr - 0xB000000 + i);
				}
			}
			else
			{
				int partLength = rom.capacity() - (pAddr - 0xB000000);
				for (int i = 0; i < partLength; i++)
				{
					dma.array()[offset + i] = rom.get(pAddr - 0xB000000 + i);
				}
				for (int i = partLength; i < length - partLength; i++)
				{
					dma.array()[offset + i] = (byte) 0;
				}
			}
			return;
		}
		if (debug)
		{
			System.err.printf("PI_DMA_WRITE not in ROM\n");
		}
	}

	@Override
	public void write8bit(int pAddr, byte value)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void write32bit(int pAddr, int value)
	{
		if (rom == null)
		{
			System.err.println("ROM storeWord: " + Integer.toHexString(pAddr));
			return;
		}

		if (pAddr < 0x1000000)
		{
			// Cartridge Domain 2 Address 1
		}
		else if (pAddr < 0x3000000)
		{
			// Cartridge Domain 1 Address 1
			System.err.println("ROM storeWord Domain 1 Address 1: " + Integer.toHexString(pAddr));
			return;
		}
		else if (pAddr < 0xB000000)
		{
			// Cartridge Domain 2 Address 2
			if (pAddr == 0x3010000)
			{
				flashRamUsed = true;
			}
			flashRam.write32bit((pAddr - 0x3000000) >> 2, value);
		}
		else if (pAddr < 0x11000000)
		{
			// Cartridge Domain 1 Address 2
			if ((pAddr - 0xB000000) < rom.capacity())
			{
				writtenToRom = true;
				wroteToRom = value;
			}
			else
			{
				System.err.println("Illegal ROM Memory SW: " + Integer.toHexString(pAddr));
				return;
			}
		}
	}

	@Override
	public void writeDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		if (rom == null)
		{
			System.err.println("ROM storeByteDma: " + Integer.toHexString(pAddr));
			return;
		}

		if (pAddr >= 0x03000000 && pAddr <= 0x03010000)
		{
			if (flashRamUsed)
			{
				((BusDMA) flashRam).writeDMA(pAddr - 0x03000000, dma, offset, length);
			}
			else
			{
				((BusDMA) sram).writeDMA(pAddr - 0x03000000, dma, offset, length);
			}
			return;
		}
		if (flashRamUsed)
		{
			System.err.printf("**** FLashRam DMA Read address %X *****\n", pAddr);
			return;
		}
		System.err.printf("PI_DMA_READ where are you dmaing to ?\n");
		return;
	}

	// Private Methods /////////////////////////////////////////////////////////

	private int getCicChipID()
	{
		long crc = 0;

		for (int count = 0x40; count < 0x1000; count += 4)
			crc += (((long) rom.getInt(count)) & 0xFFFFFFFFL);

		if (crc == 0x000000D0027FDF31L)
			return 1;
		else if (crc == 0x000000CFFB631223L)
			return 1;
		else if (crc == 0x000000D057C85244L)
			return 2;
		else if (crc == 0x000000D6497E414BL)
			return 3;
		else if (crc == 0x0000011A49F60E96L)
			return 5;
		else if (crc == 0x000000D6D5BE5580L)
			return 6;
		else
			return -1;
	}

	private boolean isValidRomImage(ByteBuffer test)
	{
		if (test.getInt(0) == 0x40123780)
		{
			if (debug) System.out.printf("ROM format: 0x40123780\n");
			return true;
		}
		if (test.getInt(0) == 0x12408037)
		{
			if (debug) System.out.printf("ROM format: 0x12408037\n");
			return true;
		}
		if (test.getInt(0) == 0x80371240)
		{
			if (debug) System.out.printf("ROM format: 0x80371240\n");
			return true;
		}
		return false;
	}

	private ByteBuffer loadDataFromRomFile(String fileName)
	{
		File file = new File(fileName);
		ByteBuffer test = ByteBuffer.allocate(4);
		test.order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer tmpData = null;
		int romSize = 0;

		if (file.getName().substring(file.getName().lastIndexOf('.')).equalsIgnoreCase(".zip"))
		{
			if (debug) System.out.println("Opening compressed ROM.");
			try
			{
				boolean foundRom = false;
				FileInputStream fis = new FileInputStream(file);
				ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null)
				{
					int counter;
					byte[] data = new byte[BUFFER];
					int size = (int) entry.getSize();
					byte[] tmpbuff = new byte[size];
					int pos = 0;
					while ((counter = zis.read(data, 0, BUFFER)) != -1)
					{
						System.arraycopy(data, 0, tmpbuff, pos, counter);
						pos += counter;
					}

					System.arraycopy(tmpbuff, 0, test.array(), 0, 4);
					if (isValidRomImage(test))
					{
						romSize = tmpbuff.length;
						tmpData = ByteBuffer.allocate(romSize);
						tmpData.order(ByteOrder.LITTLE_ENDIAN);
						System.arraycopy(tmpbuff, 0, tmpData.array(), 0, romSize);
						foundRom = true;
						break;
					}
				}
				zis.close();
				if (!foundRom)
				{
					System.err.println("No valid rom image found in zipfile: " + file);
					return null;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			if (debug) System.out.println("Opening UNcompressed ROM.");
			try
			{
				RandomAccessFile hFile;
				hFile = new RandomAccessFile(file, "r");
				hFile.read(test.array(), 0, 4);
				if (!isValidRomImage(test))
				{
					hFile.close();
					System.err.printf("Not a valid rom image: %X\n", test.getInt(0));
					return null;
				}
				hFile.seek(0);
				romSize = (int) hFile.length();
				tmpData = ByteBuffer.allocate(romSize);
				tmpData.order(ByteOrder.LITTLE_ENDIAN);

				hFile.read(tmpData.array());

				hFile.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return null;
			}
		}

		byte[] data = tmpData.array();
		switch (tmpData.getInt(0))
		{
		case 0x12408037:
			for (int count = 0; count < romSize; count += 4)
			{
				data[count] ^= data[count + 2];
				data[count + 2] ^= data[count];
				data[count] ^= data[count + 2];
				data[count + 1] ^= data[count + 3];
				data[count + 3] ^= data[count + 1];
				data[count + 1] ^= data[count + 3];
			}
			break;
		case 0x40123780:
			for (int count = 0; count < romSize; count += 4)
			{
				data[count] ^= data[count + 3];
				data[count + 3] ^= data[count];
				data[count] ^= data[count + 3];
				data[count + 1] ^= data[count + 2];
				data[count + 2] ^= data[count + 1];
				data[count + 1] ^= data[count + 2];
			}
			break;
		case 0x80371240:
			break;
		}

		// TMP
		for (int count = 0; count < romSize; count += 4)
		{
			data[count] ^= data[count + 3];
			data[count + 3] ^= data[count];
			data[count] ^= data[count + 3];
			data[count + 1] ^= data[count + 2];
			data[count + 2] ^= data[count + 1];
			data[count + 1] ^= data[count + 2];
		}
		tmpData.order(ByteOrder.BIG_ENDIAN);

		return tmpData;
	}
}
