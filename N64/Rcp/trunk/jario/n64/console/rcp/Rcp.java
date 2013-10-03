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

package jario.n64.console.rcp;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import jario.hardware.Configurable;
import jario.hardware.Hardware;

public class Rcp implements Hardware, Configurable
{
	private static final int CLK_TIMER0_PORT = 0;
	private static final int CLK_TIMER1_PORT = 1;
	private static final int CLK_TIMER2_PORT = 2;
	private static final int CLK_TIMER3_PORT = 3;

	private static final int SP_RDRAM_PORT = 0;
	private static final int SP_MIPS_PORT = 1;
	private static final int SP_DATA_PORT = 2;
	private static final int SP_TIMING_PORT = 3;

	private static final int MI_RDRAM_PORT = 0;
	private static final int MI_SP_PORT = 1;
	private static final int MI_DP_PORT = 2;
	private static final int MI_CPU_PORT = 3;
	private static final int MI_VI_PORT = 4;
	private static final int MI_AI_PORT = 5;
	private static final int MI_PI_PORT = 6;
	private static final int MI_RI_PORT = 7;
	private static final int MI_SI_PORT = 8;
	private static final int MI_CART_PORT = 9;
	private static final int MI_PIF_PORT = 10;

	private static final int VI_RDRAM_PORT = 0;
	private static final int VI_MIPS_PORT = 1;
	private static final int VI_DATA_PORT = 2;
	private static final int VI_TIMING_PORT = 3;

	private static final int AI_RDRAM_PORT = 0;
	private static final int AI_MIPS_PORT = 1;
	private static final int AI_DATA_PORT = 2;
	private static final int AI_TIMING_PORT = 3;

	private static final int PI_RDRAM_PORT = 0;
	private static final int PI_MIPS_PORT = 1;
	private static final int PI_DATA_PORT = 2;
	private static final int PI_TIMING_PORT = 3;

	private static final int SI_RDRAM_PORT = 0;
	private static final int SI_MIPS_PORT = 1;
	private static final int SI_DATA_PORT = 2;
	private static final int SI_TIMING_PORT = 3;

	private Hardware timing;
	private Hardware sp;
	private Hardware dp;
	private Hardware mi;
	private Hardware vi;
	private Hardware ai;
	private Hardware pi;
	private Hardware ri;
	private Hardware si;

	public Rcp()
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

			timing = (Hardware) Class.forName(prop.getProperty("RCP_TIMER", "RCP_TIMER"), true, loader).newInstance();
			sp = (Hardware) Class.forName(prop.getProperty("SIGNAL_PROCESSOR", "SIGNAL_PROCESSOR"), true, loader).newInstance();
			dp = (Hardware) Class.forName(prop.getProperty("DISPLAY_PROCESSOR", "DISPLAY_PROCESSOR"), true, loader).newInstance();
			mi = (Hardware) Class.forName(prop.getProperty("MIPS_INTERFACE", "MIPS_INTERFACE"), true, loader).newInstance();
			vi = (Hardware) Class.forName(prop.getProperty("VIDEO_INTERFACE", "VIDEO_INTERFACE"), true, loader).newInstance();
			ai = (Hardware) Class.forName(prop.getProperty("AUDIO_INTERFACE", "AUDIO_INTERFACE"), true, loader).newInstance();
			pi = (Hardware) Class.forName(prop.getProperty("PARALLEL_INTERFACE", "PARALLEL_INTERFACE"), true, loader).newInstance();
			ri = (Hardware) Class.forName(prop.getProperty("RDRAM_INTERFACE", "RDRAM_INTERFACE"), true, loader).newInstance();
			si = (Hardware) Class.forName(prop.getProperty("SERIAL_INTERFACE", "SERIAL_INTERFACE"), true, loader).newInstance();
		}
		catch (Exception e)
		{
			System.err.println("Missing resources.");
			e.printStackTrace();
			return;
		}

		timing.connect(CLK_TIMER0_PORT, null);
		timing.connect(CLK_TIMER1_PORT, si);
		timing.connect(CLK_TIMER2_PORT, pi);
		timing.connect(CLK_TIMER3_PORT, vi);

		sp.connect(SP_RDRAM_PORT, null);
		sp.connect(SP_MIPS_PORT, mi);
		sp.connect(SP_DATA_PORT, dp);
		sp.connect(SP_TIMING_PORT, null);

		dp.connect(SP_RDRAM_PORT, null);
		dp.connect(SP_MIPS_PORT, mi);
		dp.connect(SP_DATA_PORT, vi);
		dp.connect(SP_TIMING_PORT, null);

		mi.connect(MI_SP_PORT, sp);
		mi.connect(MI_DP_PORT, dp);
		mi.connect(MI_CPU_PORT, null);
		mi.connect(MI_VI_PORT, vi);
		mi.connect(MI_AI_PORT, ai);
		mi.connect(MI_PI_PORT, pi);
		mi.connect(MI_RI_PORT, ri);
		mi.connect(MI_SI_PORT, si);
		mi.connect(MI_CART_PORT, null);
		mi.connect(MI_RDRAM_PORT, null);
		mi.connect(MI_PIF_PORT, null);

		vi.connect(VI_RDRAM_PORT, null);
		vi.connect(VI_MIPS_PORT, mi);
		vi.connect(VI_DATA_PORT, null);
		vi.connect(VI_TIMING_PORT, timing);

		ai.connect(AI_RDRAM_PORT, null);
		ai.connect(AI_MIPS_PORT, mi);
		ai.connect(AI_DATA_PORT, null);
		ai.connect(AI_TIMING_PORT, null);

		pi.connect(PI_RDRAM_PORT, null);
		pi.connect(PI_MIPS_PORT, mi);
		pi.connect(PI_DATA_PORT, null);
		pi.connect(PI_TIMING_PORT, timing);

		si.connect(SI_RDRAM_PORT, null);
		si.connect(SI_MIPS_PORT, mi);
		si.connect(SI_DATA_PORT, null);
		si.connect(SI_TIMING_PORT, timing);
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0: // not used
			break;
		case 1: // cart
			if (bus == null)
			{ // cartridge removed
				vi.reset();
				ai.reset();
				break;
			}
			pi.connect(PI_DATA_PORT, bus);
			mi.connect(MI_CART_PORT, bus);
			vi.reset();
			ai.reset();
			break;
		case 2: // pif
			mi.connect(MI_PIF_PORT, bus);
			si.connect(SI_DATA_PORT, bus);
			break;
		case 3: // cpu
			if (bus != null)
			{
				mi.connect(MI_CPU_PORT, bus);
			}
			break;
		case 4: // rdram
			sp.connect(SP_RDRAM_PORT, bus);
			dp.connect(SP_RDRAM_PORT, bus);
			mi.connect(MI_RDRAM_PORT, bus);
			vi.connect(VI_RDRAM_PORT, bus);
			ai.connect(AI_RDRAM_PORT, bus);
			pi.connect(PI_RDRAM_PORT, bus);
			si.connect(SI_RDRAM_PORT, bus);
			break;
		case 5:
			break;
		case 6: // dac
			vi.connect(VI_DATA_PORT, bus);
			ai.connect(AI_DATA_PORT, bus);
			break;
		}
	}

	@Override
	public void reset()
	{
		timing.reset();
		sp.reset();
		dp.reset();
		mi.reset();
		vi.reset();
		ai.reset();
		pi.reset();
		ri.reset();
		si.reset();
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("framelimit")) return ((Configurable) vi).readConfig("framelimit");
		else if (key.equals("framebuffer")) return ((Configurable) vi).readConfig("framebuffer");
		else if (key.equals("MIPS")) return mi; // for performance and bios
		else if (key.equals("TIMER")) return timing; // for performance (should be moved out of the rcp anyway)
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("framelimit")) ((Configurable) vi).writeConfig("framelimit", value);
		else if (key.equals("framebuffer")) ((Configurable) vi).writeConfig("framebuffer", value);
	}
}
