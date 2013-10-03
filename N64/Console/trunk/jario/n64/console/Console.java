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

package jario.n64.console;

import jario.hardware.Bus32bit;
import jario.hardware.Bus64bit;
import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

public class Console implements Hardware, Configurable
{
	private static final int Default_CountPerOp = 2;

	private static final int PIF_CONTROLLER1_PORT = 0;
	private static final int PIF_CONTROLLER2_PORT = 1;
	private static final int PIF_CONTROLLER3_PORT = 2;
	private static final int PIF_CONTROLLER4_PORT = 3;
	private static final int PIF_EEPROM_PORT = 4;

	private static final int DAC_VIDEO_PORT = 0;
	private static final int DAC_AUDIO_PORT = 1;

	private static final int CPU_DATA_PORT = 4;
	private static final int CPU_TIMING_PORT = 5;
	private static final int CPU_CLOCK_REG = 33;
	
	private static final int RCP_CART_PORT = 1;
	private static final int RCP_PIF_PORT = 2;
	private static final int RCP_MIPS_PORT = 3;
	private static final int RCP_RDRAM_PORT = 4;
	private static final int RCP_DAC_PORT = 6;

	private Hardware cpu;
	private Hardware rcp;
	private Hardware rdram;
	private Hardware pif;
	private Hardware dac;
	private Hardware cartridge;
	private Hardware[] controllers;

	public Console()
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

			cpu = (Hardware) Class.forName(prop.getProperty("CPU", "CPU"), true, loader).newInstance();
			rcp = (Hardware) Class.forName(prop.getProperty("RCP", "RCP"), true, loader).newInstance();
			rdram = (Hardware) Class.forName(prop.getProperty("RDRAM", "RDRAM"), true, loader).newInstance();
			pif = (Hardware) Class.forName(prop.getProperty("PIF", "PIF"), true, loader).newInstance();
			dac = (Hardware) Class.forName(prop.getProperty("DAC", "DAC"), true, loader).newInstance();
		}
		catch (Exception e)
		{
			System.err.println("Missing resources.");
			e.printStackTrace();
			return;
		}

		cpu.connect(CPU_DATA_PORT, (Hardware)((Configurable)rcp).readConfig("MIPS"));
		cpu.connect(CPU_TIMING_PORT, (Hardware)((Configurable)rcp).readConfig("TIMER"));
		
		rcp.connect(RCP_PIF_PORT, pif);
		rcp.connect(RCP_MIPS_PORT, cpu);
		rcp.connect(RCP_RDRAM_PORT, rdram);
		rcp.connect(RCP_DAC_PORT, dac);
		
		dac.connect(6, rdram);
		controllers = new Hardware[4];
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0: // not used
			break;
		case 1: // controller1
			if (bus == null)
			{ // controller removed
				pif.connect(PIF_CONTROLLER1_PORT, null);
				if (controllers[0] != null)
					controllers[0].reset();
				break;
			}
			controllers[0] = bus;
			controllers[0].reset();
			pif.connect(PIF_CONTROLLER1_PORT, null);
			pif.connect(PIF_CONTROLLER1_PORT, controllers[0]);
			break;
		case 2: // controller2
			if (bus == null)
			{ // controller removed
				pif.connect(PIF_CONTROLLER2_PORT, null);
				if (controllers[1] != null)
					controllers[1].reset();
				break;
			}
			controllers[1] = bus;
			controllers[1].reset();
			pif.connect(PIF_CONTROLLER2_PORT, null);
			pif.connect(PIF_CONTROLLER2_PORT, controllers[1]);
			break;
		case 3: // controller3
			if (bus == null)
			{ // controller removed
				pif.connect(PIF_CONTROLLER3_PORT, null);
				if (controllers[2] != null)
					controllers[2].reset();
				break;
			}
			controllers[2] = bus;
			controllers[2].reset();
			pif.connect(PIF_CONTROLLER3_PORT, null);
			pif.connect(PIF_CONTROLLER3_PORT, controllers[2]);
			break;
		case 4: // controller4
			if (bus == null)
			{ // controller removed
				pif.connect(PIF_CONTROLLER4_PORT, null);
				if (controllers[3] != null)
					controllers[3].reset();
				break;
			}
			controllers[3] = bus;
			controllers[3].reset();
			pif.connect(PIF_CONTROLLER4_PORT, null);
			pif.connect(PIF_CONTROLLER4_PORT, controllers[3]);
			break;
		case 5: // cartridge
			if (bus == null)
			{ // cartridge removed
				((Clockable) cpu).clock(0L); // stop cpu
				if (cartridge != null)
				{
					cartridge.reset();
				}
				cpu.reset();
				rcp.reset();
				pif.reset();
				dac.reset();
				break;
			}

			cartridge = bus;
			cartridge.reset();

			pif.connect(PIF_EEPROM_PORT, (Hardware) ((Configurable) cartridge).readConfig("S-DAT"));

			// Could be made ROM dependent
			int countPerOp = -1;
			if (countPerOp < 1)
			{
				countPerOp = Default_CountPerOp;
			}
			if (countPerOp > 6)
			{
				countPerOp = Default_CountPerOp;
			}

			((Bus32bit) cpu).write32bit(CPU_CLOCK_REG, countPerOp);

			runBIOS((Bus8bit) cartridge, (Bus32bit) ((Configurable) rcp).readConfig("MIPS"), (Bus64bit) cpu);

			rcp.connect(RCP_CART_PORT, bus);

			((Clockable) cpu).clock(1L); // start cpu
			break;
		case 6: // video
			dac.connect(DAC_VIDEO_PORT, bus);
			break;
		case 7: // audio
			dac.connect(DAC_AUDIO_PORT, bus);
			break;
		case 8: // reserved a/v
			break;
		case 9: // reserved a/v
			break;
		case 10: // reserved a/v
			break;
		case 11: // reserved a/v
			break;
		}
	}

	@Override
	public void reset()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("instructioncache")) return ((Configurable) cpu).readConfig("instructioncache");
		else if (key.equals("framelimit")) return ((Configurable) rcp).readConfig("framelimit");
		else if (key.equals("framebuffer")) return ((Configurable) rcp).readConfig("framebuffer");
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("instructioncache")) ((Configurable) cpu).writeConfig("instructioncache", value);
		else if (key.equals("framelimit")) ((Configurable) rcp).writeConfig("framelimit", value);
		else if (key.equals("framebuffer")) ((Configurable) rcp).writeConfig("framebuffer", value);
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void runBIOS(Bus8bit rom, Bus32bit mem, Bus64bit cpu)
	{
		switch (rom.read8bit(0x3E + 0xB000000))
		{
		case 0x44: // Germany
		case 0x46: // french
		case 0x49: // Italian
		case 0x50: // Europe
		case 0x53: // Spanish
		case 0x55: // Australia
		case 0x58: // X (PAL)
		case 0x59: // X (PAL)
			break;
		case 0: // None
		case 0x37: // 7 (Beta)
		case 0x41: // ????
		case 0x45: // USA
		case 0x4A: // Japan
			break;
		default:
			System.err.printf("Unknown country in LoadPifRom\n");
		}
		// boolean usePif = false;

		int country = rom.read8bit(0x3E + 0xB000000);
		int cicChip = (Integer) ((Configurable) rom).readConfig("cic");
		if (cicChip < 0)
		{
			System.err.printf("Unknown Cic Chip: %d", cicChip);
			cicChip = 2;
		}

		// if (usePif) {
		// System.out.println("Using PIF");
		// // cpu.writeRegister(32, 0xBFC00000);
		// ((Bus32bit)cpu).write32bit(32, 0xBFC00000);
		// switch (cicChip) {
		// case 1:
		// write32bit(36+0x7C0, 0x00063F3F);
		// break;
		// case 2:
		// write32bit(36+0x7C0, 0x00023F3F);
		// break;
		// case 3:
		// write32bit(36+0x7C0, 0x0002783F);
		// break;
		// case 5:
		// write32bit(36+0x7C0, 0x0002913F);
		// break;
		// case 6:
		// write32bit(36+0x7C0, 0x0002853F);
		// break;
		// }
		// }
		// else
		{
			for (int i = 0; i < 0xFBC; i++)
				((Bus8bit) mem).write8bit(0x04000040 + i, rom.read8bit((0x040 + 0xB000000) + i));
			((Bus32bit) cpu).write32bit(32, 0xA4000040);
	
			cpu.write64bit(0, 0x00000000);
			cpu.write64bit(6, 0xA4001F0C);
			cpu.write64bit(7, 0xA4001F08);
			cpu.write64bit(8, 0x000000C0);
			cpu.write64bit(9, 0x00000000);
			cpu.write64bit(10, 0x00000040);
			cpu.write64bit(11, 0xA4000040);
			cpu.write64bit(16, 0x00000000);
			cpu.write64bit(17, 0x00000000);
			cpu.write64bit(18, 0x00000000);
			cpu.write64bit(19, 0x00000000);
			cpu.write64bit(21, 0x00000000);
			cpu.write64bit(26, 0x00000000);
			cpu.write64bit(27, 0x00000000);
			cpu.write64bit(28, 0x00000000);
			cpu.write64bit(29, 0xA4001FF0);
			cpu.write64bit(30, 0x00000000);
	
			switch (country)
			{
			case 0x44: // Germany
			case 0x46: // french
			case 0x49: // Italian
			case 0x50: // Europe
			case 0x53: // Spanish
			case 0x55: // Australia
			case 0x58: // ????
			case 0x59: // X (PAL)
				switch (cicChip)
				{
				case 2:
					cpu.write64bit(5, 0xC0F1D859);
					cpu.write64bit(14, 0x2DE108EA);
					cpu.write64bit(24, 0x00000000);
					break;
				case 3:
					cpu.write64bit(5, 0xD4646273);
					cpu.write64bit(14, 0x1AF99984);
					cpu.write64bit(24, 0x00000000);
					break;
				case 5:
					mem.write32bit(0x04001004, 0xBDA807FC);
					cpu.write64bit(5, 0xDECAAAD1);
					cpu.write64bit(14, 0x0CF85C13);
					cpu.write64bit(24, 0x00000002);
					break;
				case 6:
					cpu.write64bit(5, 0xB04DC903);
					cpu.write64bit(14, 0x1AF99984);
					cpu.write64bit(24, 0x00000002);
					break;
				}
				cpu.write64bit(20, 0x00000000);
				cpu.write64bit(23, 0x00000006);
				cpu.write64bit(31, 0xA4001554);
				break;
			case 0x37: // 7 (Beta)
			case 0x41: // ????
			case 0x45: // USA
			case 0x4A: // Japan
			default:
				switch (cicChip)
				{
				case 2:
					cpu.write64bit(5, 0xC95973D5);
					cpu.write64bit(14, 0x2449A366);
					break;
				case 3:
					cpu.write64bit(5, 0x95315A28);
					cpu.write64bit(14, 0x5BACA1DF);
					break;
				case 5:
					mem.write32bit(0x04001004, 0x8DA807FC);
					cpu.write64bit(5, 0x5493FB9A);
					cpu.write64bit(14, 0xC2C20384);
				case 6:
					cpu.write64bit(5, 0xE067221F);
					cpu.write64bit(14, 0x5CD2B70F);
					break;
				}
				cpu.write64bit(20, 0x00000001);
				cpu.write64bit(23, 0x00000000);
				cpu.write64bit(24, 0x00000003);
				cpu.write64bit(31, 0xA4001550);
			}
	
			switch (cicChip)
			{
			case 1:
				cpu.write64bit(22, 0x0000003F);
				break;
			case 2:
				cpu.write64bit(1, 0x00000001);
				cpu.write64bit(2, 0x0EBDA536);
				cpu.write64bit(3, 0x0EBDA536);
				cpu.write64bit(4, 0x0000A536);
				cpu.write64bit(12, 0xED10D0B3);
				cpu.write64bit(13, 0x1402A4CC);
				cpu.write64bit(15, 0x3103E121);
				cpu.write64bit(22, 0x0000003F);
				cpu.write64bit(25, 0x9DEBB54F);
				break;
			case 3:
				cpu.write64bit(1, 0x00000001);
				cpu.write64bit(2, 0x49A5EE96);
				cpu.write64bit(3, 0x49A5EE96);
				cpu.write64bit(4, 0x0000EE96);
				cpu.write64bit(12, 0xCE9DFBF7);
				cpu.write64bit(13, 0xCE9DFBF7);
				cpu.write64bit(15, 0x18B63D28);
				cpu.write64bit(22, 0x00000078);
				cpu.write64bit(25, 0x825B21C9);
				break;
			case 5:
				mem.write32bit(0x04001000, 0x3C0DBFC0);
				mem.write32bit(0x04001008, 0x25AD07C0);
				mem.write32bit(0x0400100C, 0x31080080);
				mem.write32bit(0x04001010, 0x5500FFFC);
				mem.write32bit(0x04001014, 0x3C0DBFC0);
				mem.write32bit(0x04001018, 0x8DA80024);
				mem.write32bit(0x0400101C, 0x3C0BB000);
				cpu.write64bit(1, 0x00000000);
				cpu.write64bit(2, 0xF58B0FBF);
				cpu.write64bit(3, 0xF58B0FBF);
				cpu.write64bit(4, 0x00000FBF);
				cpu.write64bit(12, 0x9651F81E);
				cpu.write64bit(13, 0x2D42AAC5);
				cpu.write64bit(15, 0x56584D60);
				cpu.write64bit(22, 0x00000091);
				cpu.write64bit(25, 0xCDCE565F);
				break;
			case 6:
				cpu.write64bit(1, 0x00000000);
				cpu.write64bit(2, 0xA95930A4);
				cpu.write64bit(3, 0xA95930A4);
				cpu.write64bit(4, 0x000030A4);
				cpu.write64bit(12, 0xBCB59510);
				cpu.write64bit(13, 0xBCB59510);
				cpu.write64bit(15, 0x7A3C07F4);
				cpu.write64bit(22, 0x00000085);
				cpu.write64bit(25, 0x465E3F72);
				break;
			}
		}
		System.out.printf("BIOS complete\n");
	}
}
