/**
 * Copyright 2009, 2013 Jason LaDere
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.Properties;

public class Cartridge implements Hardware, Bus8bit, Bus32bit, BusDMA, Configurable
{
	private static final int CART_SRAM_PORT = 0;
	private static final int CART_FLASHRAM_PORT = 1;

	private Hardware cartrom;
	private final Bus8bit cartrom8bit;
	private final Bus32bit cartrom32bit;
	private final BusDMA cartromDMA;
	private Hardware eeprom;
	private Hardware sram;
	private Hardware flashram;

	public Cartridge()
	{
		try
		{
			File dir = new File("components" + File.separator);
			File file = new File("components.properties");
			ClassLoader loader = this.getClass().getClassLoader();
			Properties prop = new Properties();
			try
			{
				if (dir.exists() && dir.listFiles().length > 0)
				{
					File[] files = dir.listFiles();
					URL[] urls = new URL[files.length];
					for (int i = 0; i < files.length; i++) urls[i] = files[i].toURI().toURL();
					loader = new URLClassLoader(urls, this.getClass().getClassLoader());
				}
				URL url = file.exists() ? file.toURI().toURL() : loader.getResource("resources" + File.separator + "components.properties");
				if (url != null) prop.load(url.openStream());
			}
			catch (IOException e)
			{
			}

			cartrom = (Hardware) Class.forName(prop.getProperty("CARTRIDGE_ROM", "CARTRIDGE_ROM"), true, loader).newInstance();
			eeprom = (Hardware) Class.forName(prop.getProperty("EEPROM", "EEPROM"), true, loader).newInstance();
			sram = (Hardware) Class.forName(prop.getProperty("SRAM", "SRAM"), true, loader).newInstance();
			flashram = (Hardware) Class.forName(prop.getProperty("FLASHRAM", "FLASHRAM"), true, loader).newInstance();
		}
		catch (Exception e)
		{
			System.err.println("Missing resources.");
			e.printStackTrace();
		}

		cartrom.connect(CART_SRAM_PORT, sram);
		cartrom.connect(CART_FLASHRAM_PORT, flashram);

		cartrom8bit = (Bus8bit) cartrom;
		cartrom32bit = (Bus32bit) cartrom;
		cartromDMA = (BusDMA) cartrom;
	}

	public void connect(int port, Hardware bus)
	{
	}

	public void reset()
	{
		cartrom.reset();
		eeprom.reset();
	}

	@Override
	public final byte read8bit(int address)
	{
		return cartrom8bit.read8bit(address);
	}

	@Override
	public final void write8bit(int address, byte data)
	{
		cartrom8bit.write8bit(address, data);
	}

	@Override
	public final int read32bit(int address)
	{
		return cartrom32bit.read32bit(address);
	}

	@Override
	public final void write32bit(int address, int data)
	{
		cartrom32bit.write32bit(address, data);
	}

	@Override
	public final void readDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		cartromDMA.readDMA(pAddr, dma, offset, length);
	}

	@Override
	public final void writeDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		cartromDMA.writeDMA(pAddr, dma, offset, length);
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("S-DAT")) return eeprom;
		else if (key.equals("cic")) return ((Configurable) cartrom).readConfig("cic");
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("romfile"))
		{
			((Configurable) cartrom).writeConfig("romfile", value);
		}
		else if (key.equals("savefile"))
		{
			((Configurable) eeprom).writeConfig("file", value + ".eep");
			((Configurable) sram).writeConfig("file", value + ".sra");
			((Configurable) flashram).writeConfig("file", value + ".fla");
		}
	}
}
