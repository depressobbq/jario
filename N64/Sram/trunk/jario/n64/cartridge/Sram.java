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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class Sram implements Hardware, BusDMA, Configurable
{
	private static final String AUTO_SAVE_DIR = "./save/default.sra";

	private RandomAccessFile hSramFile;
	private File file;

	public Sram()
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
		if (hSramFile == null)
		{
			if (!loadSram())
				return;
		}
		try
		{
			hSramFile.seek(pAddr);
			hSramFile.read(dma.array(), offset, length);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void writeDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		if (hSramFile == null)
		{
			if (!loadSram()) { return; }
		}
		try
		{
			hSramFile.seek(pAddr);
			hSramFile.write(dma.array(), offset, length);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void close()
	{
		if (hSramFile != null)
		{
			try
			{
				hSramFile.close();
				hSramFile = null;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private boolean loadSram()
	{
		try
		{
			hSramFile = new RandomAccessFile(file, "rwd");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
