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

package jario.n64.accessories;

import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Mempak implements Hardware, Bus32bit, BusDMA, Configurable
{
	private static final String AUTO_SAVE_DIR = "./save/default.mpk";
	private static final int PAK_TYPE_MEMPAK = 2;

	private byte[] mempak = new byte[4 * 0x8000];
	private RandomAccessFile hMempakFile;
	private File file;
	private int control;

	public Mempak()
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
			return PAK_TYPE_MEMPAK;
		case 1:
			return control;
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch (reg)
		{
		case 1:
			control = value;
			break;
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
		if (pAddr == 0x8001)
		{
			Arrays.fill(dma.array(), offset, offset + length, (byte) 0);
			return;
		}
		pAddr &= 0xFFE0;

		if (pAddr <= 0x7FE0)
		{
			if (hMempakFile == null)
			{
				loadMempak();
			}
			System.arraycopy(mempak, control * 0x8000 + pAddr, dma.array(), offset, length);
		}
		else
		{
			/* Rumble pack area */
			Arrays.fill(dma.array(), offset, offset + length, (byte) 0);
		}
	}

	@Override
	public void writeDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		if (pAddr == 0x8001) { return; }

		pAddr &= 0xFFE0;
		if (pAddr <= 0x7FE0)
		{
			System.arraycopy(dma.array(), offset, mempak, control * 0x8000 + pAddr, length);

			if (hMempakFile == null && !loadMempak()) { return; }

			try
			{
				hMempakFile.seek(control * 0x8000);
				hMempakFile.write(mempak, control * 0x8000, 0x8000);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			/* Rumble pack area */
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void close()
	{
		if (hMempakFile != null)
		{
			try
			{
				hMempakFile.close();
				hMempakFile = null;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private boolean loadMempak()
	{
		int[] initilize =
		{
				0x81, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0C, 0x0D, 0x0E, 0x0F,
				0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
				0xFF, 0xFF, 0xFF, 0xFF, 0x05, 0x1A, 0x5F, 0x13, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01, 0xFF, 0x66, 0x25, 0x99, 0xCD,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0xFF, 0xFF, 0xFF, 0xFF, 0x05, 0x1A, 0x5F, 0x13, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01, 0xFF, 0x66, 0x25, 0x99, 0xCD,
				0xFF, 0xFF, 0xFF, 0xFF, 0x05, 0x1A, 0x5F, 0x13, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01, 0xFF, 0x66, 0x25, 0x99, 0xCD,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0xFF, 0xFF, 0xFF, 0xFF, 0x05, 0x1A, 0x5F, 0x13, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01, 0xFF, 0x66, 0x25, 0x99, 0xCD,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x71, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03, 0x00, 0x03,
		};
		for (int count = 0; count < 4; count++)
		{
			for (int count2 = 0; count2 < 0x8000; count2 += 2)
			{
				mempak[count * 0x8000 + count2] = 0x00;
				mempak[count * 0x8000 + count2 + 1] = 0x03;
			}
			for (int i = 0, m = count * 0x8000; i < initilize.length; i++, m++)
			{
				mempak[m] = (byte) initilize[i];
			}
		}

		try
		{
			hMempakFile = new RandomAccessFile(file, "rwd");
			hMempakFile.seek(0);
			hMempakFile.read(mempak, 0, mempak.length);
			hMempakFile.write(mempak, 0, mempak.length);
			return true;
		}
		catch (FileNotFoundException e)
		{

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		hMempakFile = null;
		return false;
	}
}
