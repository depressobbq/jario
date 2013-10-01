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

import jario.hardware.BusDMA;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Eeprom implements Hardware, BusDMA, Configurable
{
	private static final String AUTO_SAVE_DIR = "./save/default.eep";

	private RandomAccessFile hEepromFile;
	private File file;
	private byte[] eeprom = new byte[0x800];

	public Eeprom()
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
		if (hEepromFile == null)
			loadEeprom();

		for (int i = 0; i < length; i++)
			dma.array()[offset + i] = eeprom[pAddr * 8 + i];
	}

	@Override
	public void writeDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		for (int i = 0; i < length; i++)
			eeprom[pAddr * 8 + i] = dma.array()[offset + i];

		if (hEepromFile == null && !loadEeprom())
			return;

		try
		{
			hEepromFile.seek(pAddr * 8);
			hEepromFile.write(dma.array(), offset, length);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void close()
	{
		if (hEepromFile != null)
		{
			try
			{
				hEepromFile.close();
				hEepromFile = null;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private boolean loadEeprom()
	{
		try
		{
			hEepromFile = new RandomAccessFile(file, "rwd");
			Arrays.fill(eeprom, (byte) 0);
			hEepromFile.read(eeprom, 0, eeprom.length);
			return true;
		}
		catch (FileNotFoundException e)
		{

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		hEepromFile = null;
		return false;
	}
}
