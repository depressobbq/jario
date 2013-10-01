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

import jario.hardware.Bus16bit;
import jario.hardware.Bus32bit;
import jario.hardware.Bus64bit;
import jario.hardware.Bus8bit;
import jario.hardware.BusDMA;
import jario.hardware.Clockable;
import jario.hardware.Hardware;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class SignalProcessor implements Hardware, Bus8bit, Bus16bit, Bus32bit, Bus64bit
{
	private static final int MI_INTR_REG = 0x04300008;

	private static final int MI_INTR_CLR_SP = 0x0001; /* Bit 0: clear SP interrupt */
	private static final int MI_INTR_SET_SP = 0x0002; /* Bit 1: set SP interrupt */
	// private static final int MI_INTR_MASK_CLR_SP = 0x0001; /* Bit 0: clear SP mask */
	// private static final int MI_INTR_MASK_SET_SP = 0x0002; /* Bit 1: set SP mask */
	// private static final int MI_INTR_MASK_SP = 0x01; /* Bit 0: SP intr mask */
	// private static final int MI_INTR_SP = 0x01; /* Bit 0: SP intr */

	private static final int DPC_STATUS_REG = 0x0410000C;
	private static final int DPC_INIT_REG = 0x0410002C;

	private static final int DPC_CLR_FREEZE = 0x0004; /* Bit 2: clear freeze */
	// private static final int DPC_SET_FREEZE = 0x0008; /* Bit 3: set freeze */

	private static final int SP_MEM_ADDR_REG = 0;
	private static final int SP_DRAM_ADDR_REG = 1;
	private static final int SP_RD_LEN_REG = 2;
	private static final int SP_WR_LEN_REG = 3;
	private static final int SP_STATUS_REG = 4;
	private static final int SP_DMA_FULL_REG = 5;
	private static final int SP_DMA_BUSY_REG = 6;
	private static final int SP_SEMAPHORE_REG = 7;
	private static final int SP_PC_REG = 8;
	// private static final int SP_IBIST_REG = 9;

	private static final int SP_CLR_HALT = 0x00001; /* Bit 0: clear halt */
	private static final int SP_SET_HALT = 0x00002; /* Bit 1: set halt */
	private static final int SP_CLR_BROKE = 0x00004; /* Bit 2: clear broke */
	private static final int SP_CLR_INTR = 0x00008; /* Bit 3: clear intr */
	private static final int SP_SET_INTR = 0x00010; /* Bit 4: set intr */
	private static final int SP_CLR_SSTEP = 0x00020; /* Bit 5: clear sstep */
	private static final int SP_SET_SSTEP = 0x00040; /* Bit 6: set sstep */
	private static final int SP_CLR_INTR_BREAK = 0x00080; /* Bit 7: clear intr on break */
	private static final int SP_SET_INTR_BREAK = 0x00100; /* Bit 8: set intr on break */
	private static final int SP_CLR_SIG0 = 0x00200; /* Bit 9: clear signal 0 */
	private static final int SP_SET_SIG0 = 0x00400; /* Bit 10: set signal 0 */
	private static final int SP_CLR_SIG1 = 0x00800; /* Bit 11: clear signal 1 */
	private static final int SP_SET_SIG1 = 0x01000; /* Bit 12: set signal 1 */
	private static final int SP_CLR_SIG2 = 0x02000; /* Bit 13: clear signal 2 */
	private static final int SP_SET_SIG2 = 0x04000; /* Bit 14: set signal 2 */
	private static final int SP_CLR_SIG3 = 0x08000; /* Bit 15: clear signal 3 */
	private static final int SP_SET_SIG3 = 0x10000; /* Bit 16: set signal 3 */
	private static final int SP_CLR_SIG4 = 0x20000; /* Bit 17: clear signal 4 */
	private static final int SP_SET_SIG4 = 0x40000; /* Bit 18: set signal 4 */
	private static final int SP_CLR_SIG5 = 0x80000; /* Bit 19: clear signal 5 */
	private static final int SP_SET_SIG5 = 0x100000; /* Bit 20: set signal 5 */
	private static final int SP_CLR_SIG6 = 0x200000; /* Bit 21: clear signal 6 */
	private static final int SP_SET_SIG6 = 0x400000; /* Bit 22: set signal 6 */
	private static final int SP_CLR_SIG7 = 0x800000; /* Bit 23: clear signal 7 */
	private static final int SP_SET_SIG7 = 0x1000000; /* Bit 24: set signal 7 */

	private static final int SP_STATUS_HALT = 0x001; /* Bit 0: halt */
	private static final int SP_STATUS_BROKE = 0x002; /* Bit 1: broke */
	private static final int SP_STATUS_DMA_BUSY = 0x004; /* Bit 2: dma busy */
	// private static final int SP_STATUS_DMA_FULL = 0x008; /* Bit 3: dma full */
	// private static final int SP_STATUS_IO_FULL = 0x010; /* Bit 4: io full */
	private static final int SP_STATUS_SSTEP = 0x020; /* Bit 5: single step */
	private static final int SP_STATUS_INTR_BREAK = 0x040; /* Bit 6: interrupt on break */
	private static final int SP_STATUS_SIG0 = 0x080; /* Bit 7: signal 0 set */
	private static final int SP_STATUS_SIG1 = 0x100; /* Bit 8: signal 1 set */
	private static final int SP_STATUS_SIG2 = 0x200; /* Bit 9: signal 2 set */
	private static final int SP_STATUS_SIG3 = 0x400; /* Bit 10: signal 3 set */
	private static final int SP_STATUS_SIG4 = 0x800; /* Bit 11: signal 4 set */
	private static final int SP_STATUS_SIG5 = 0x1000; /* Bit 12: signal 5 set */
	private static final int SP_STATUS_SIG6 = 0x2000; /* Bit 13: signal 6 set */
	private static final int SP_STATUS_SIG7 = 0x4000; /* Bit 14: signal 7 set */

	private static final int RDRAM_CAPACITY_REG = 0x03F00028;

	private static final int UCODE_RDRAM_PORT = 1;
	private static final int UCODE_SP_PORT = 2;
	private static final int UCODE_DP_PORT = 3;

	// private static final int IMEM_START = 0x00001000; // From DMEM //0x04001000;

	private static final boolean DLIST = true;
	private static final boolean ALIST = true;

	private int[] regSP = new int[10];
	private ByteBuffer dmem;

	private Bus32bit rdram;
	private Bus32bit mi;
	private Bus32bit dp;

	// Audio
	private int inst2, inst1;
	private int UCData;
	// private int UDataLen;

	private int pcStackSize;
	private int[] PC = new int[18];
	private int PCi;
	private boolean halt;
	private int rdramSize;

	private Microcode current;
	private int numMicrocodes;
	private Microcode top;
	private Microcode bottom;
	private Checksum crc32 = new Adler32();
	private int uc_crc;
	// private int uc_dcrc;
	private String uc_str;
	private ByteBuffer rdramcrc = ByteBuffer.allocate(4096);
	private Microcode[] specialMicrocodes = {
			new Microcode(Microcode.F3DWRUS, false, 0xd17906e2, "RSP SW Version: 2.0D, 04-01-96"),
			new Microcode(Microcode.F3DWRUS, false, 0x94c4c833, "RSP SW Version: 2.0D, 04-01-96"),
			new Microcode(Microcode.S2DEX, false, 0x9df31081, "RSP Gfx ucode S2DEX  1.06 Yoshitaka Yasumoto Nintendo."),
			new Microcode(Microcode.F3DDKR, false, 0x8d91244f, "Diddy Kong Racing"),
			new Microcode(Microcode.F3DDKR, false, 0x6e6fc893, "Diddy Kong Racing"),
			new Microcode(Microcode.F3DDKR, false, 0xbde9d1fb, "Jet Force Gemini"),
			new Microcode(Microcode.F3DPD, false, 0x1c4f7869, "Perfect Dark")
	};

	private class Microcode
	{
		public static final int F3D = 0;
		public static final int F3DEX = 1;
		public static final int F3DEX2 = 2;
		// public static final int L3D = 3;
		public static final int L3DEX = 4;
		public static final int L3DEX2 = 5;
		public static final int S2DEX = 6;
		public static final int S2DEX2 = 7;
		public static final int F3DPD = 8;
		public static final int F3DDKR = 9;
		public static final int F3DWRUS = 10;
		public static final int F3DEXBG = 11;
		public static final int NONE = 12;

		public int address;
		public int dataAddress;
		public short dataSize;
		public int type;
		public boolean NoN;
		public int crc;
		public String text;
		public Microcode higher;
		public Microcode lower;

		public Microcode(int type, boolean NoN, int crc, String text)
		{
			this.type = type;
			this.NoN = NoN;
			this.crc = crc;
			this.text = text;
		}
	}

	private Bus64bit[] dUcode = new Bus64bit[13];
	private Bus64bit[] aUcode = new Bus64bit[4];

	public SignalProcessor()
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

			dUcode[0] = (Bus64bit) Class.forName(prop.getProperty("UGBI00", "UGBI00"), true, loader).newInstance();
			dUcode[1] = (Bus64bit) Class.forName(prop.getProperty("UGBI01", "UGBI01"), true, loader).newInstance();
			dUcode[2] = (Bus64bit) Class.forName(prop.getProperty("UGBI02", "UGBI02"), true, loader).newInstance();
			dUcode[3] = (Bus64bit) Class.forName(prop.getProperty("UGBI03", "UGBI03"), true, loader).newInstance();
			dUcode[4] = (Bus64bit) Class.forName(prop.getProperty("UGBI04", "UGBI04"), true, loader).newInstance();
			dUcode[5] = (Bus64bit) Class.forName(prop.getProperty("UGBI05", "UGBI05"), true, loader).newInstance();
			dUcode[6] = (Bus64bit) Class.forName(prop.getProperty("UGBI06", "UGBI06"), true, loader).newInstance();
			dUcode[7] = (Bus64bit) Class.forName(prop.getProperty("UGBI07", "UGBI07"), true, loader).newInstance();
			dUcode[8] = (Bus64bit) Class.forName(prop.getProperty("UGBI08", "UGBI08"), true, loader).newInstance();
			dUcode[9] = (Bus64bit) Class.forName(prop.getProperty("UGBI09", "UGBI09"), true, loader).newInstance();
			dUcode[10] = (Bus64bit) Class.forName(prop.getProperty("UGBI10", "UGBI10"), true, loader).newInstance();
			dUcode[11] = (Bus64bit) Class.forName(prop.getProperty("UGBI11", "UGBI11"), true, loader).newInstance();
			dUcode[12] = (Bus64bit) Class.forName(prop.getProperty("UGBI12", "UGBI12"), true, loader).newInstance();
			aUcode[0] = (Bus64bit) Class.forName(prop.getProperty("UABI00", "UABI00"), true, loader).newInstance();
			aUcode[1] = (Bus64bit) Class.forName(prop.getProperty("UABI01", "UABI01"), true, loader).newInstance();
			aUcode[2] = (Bus64bit) Class.forName(prop.getProperty("UABI02", "UABI02"), true, loader).newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		regSP[SP_STATUS_REG] = 0x00000001;
		dmem = ByteBuffer.allocate(0x2000); // 8,192b
		// dmem.position(IMEM_START);
		// imem = dmem.slice();

		for (int i = 0; i < dUcode.length; i++)
			if (dUcode[i] != null)
				((Hardware) dUcode[i]).connect(UCODE_SP_PORT, this);
		for (int i = 0; i < aUcode.length; i++)
			if (aUcode[i] != null)
				((Hardware) aUcode[i]).connect(UCODE_SP_PORT, this);
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			rdram = (Bus32bit) bus;
			for (int i = 0; i < dUcode.length; i++)
				if (dUcode[i] != null)
					((Hardware) dUcode[i]).connect(UCODE_RDRAM_PORT, (Hardware) rdram);
			for (int i = 0; i < aUcode.length; i++)
				if (aUcode[i] != null)
					((Hardware) aUcode[i]).connect(UCODE_RDRAM_PORT, (Hardware) rdram);
			if (rdram != null)
			{
				rdramSize = rdram.read32bit(RDRAM_CAPACITY_REG);
			}
			break;
		case 1:
			mi = (Bus32bit) bus;
			break;
		case 2:
			dp = (Bus32bit) bus;
			if (dp != null)
			{
				top = null;
				bottom = null;
				current = null;
				numMicrocodes = 0;
				for (int i = 0; i < dUcode.length; i++)
					if (dUcode[i] != null)
						((Hardware) dUcode[i]).connect(UCODE_DP_PORT, (Hardware) dp);
			}
			break;
		case 3:
			break; // timing
		default:
			System.err.println("Attempting to connect bus on invalid port: " + port);
			break;
		}
	}

	@Override
	public void reset()
	{
		regSP[SP_STATUS_REG] = 0x00000001;
		if (dUcode[0] != null)
			((Hardware) dUcode[0]).reset();
	}

	@Override
	public byte read8bit(int pAddr)
	{
		return dmem.get(pAddr);
	}

	@Override
	public short read16bit(int pAddr)
	{
		return dmem.getShort(pAddr);
	}

	@Override
	public int read32bit(int pAddr)
	{
		if (pAddr < 0x2000)
		{
			return dmem.getInt(pAddr);
		}
		else
		{
			switch ((pAddr - 0x04040000) >> 2)
			{
			case 4:
				return regSP[SP_STATUS_REG];
			case 5:
				return regSP[SP_DMA_FULL_REG];
			case 6:
				return regSP[SP_DMA_BUSY_REG];
			case 10:
				return PC[PCi]; // DL PC
			case 11:
				if (PCi > 0)
					PCi--;
				else
					halt = true;
				return PCi;
			case 12:
				return current.NoN ? 1 : 0;
			case 0x10000:
				return regSP[SP_PC_REG];
			default:
				return 0;
			}
		}
	}

	@Override
	public long read64bit(int pAddr)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void write8bit(int pAddr, byte value)
	{
		dmem.put(pAddr, value);
	}

	@Override
	public void write16bit(int pAddr, short value)
	{
		dmem.putShort(pAddr, value);
	}

	@Override
	public void write32bit(int pAddr, int value)
	{
		if (pAddr < 0x2000)
		{
			dmem.putInt(pAddr, value);
		}
		else
		{
			if (((pAddr - 0x04040000) >> 2) < 0) { return; }
			switch ((pAddr - 0x04040000) >> 2)
			{
			case 0:
				regSP[SP_MEM_ADDR_REG] = value;
				break;
			case 1:
				regSP[SP_DRAM_ADDR_REG] = value;
				break;
			case 2:
				regSP[SP_RD_LEN_REG] = value;
				spDmaRead(regSP[SP_DRAM_ADDR_REG], regSP[SP_MEM_ADDR_REG], regSP[SP_RD_LEN_REG]);
				break;
			case 3:
				regSP[SP_WR_LEN_REG] = value;
				System.out.println("SP_DMA_WRITE");
				break;
			case 4:
				if ((value & SP_CLR_HALT) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_HALT;
				}
				if ((value & SP_SET_HALT) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_HALT;
				}
				if ((value & SP_CLR_BROKE) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_BROKE;
				}
				if ((value & SP_CLR_INTR) != 0)
				{
					mi.write32bit(MI_INTR_REG, MI_INTR_CLR_SP);
				}
				if ((value & SP_SET_INTR) != 0)
					System.err.printf("SP_SET_INTR\n");
				if ((value & SP_CLR_SSTEP) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_SSTEP;
				}
				if ((value & SP_SET_SSTEP) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_SSTEP;
				}
				if ((value & SP_CLR_INTR_BREAK) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_INTR_BREAK;
				}
				if ((value & SP_SET_INTR_BREAK) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_INTR_BREAK;
				}
				if ((value & SP_CLR_SIG0) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_SIG0;
				}
				if ((value & SP_SET_SIG0) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_SIG0;
				}
				if ((value & SP_CLR_SIG1) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_SIG1;
				}
				if ((value & SP_SET_SIG1) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_SIG1;
				}
				if ((value & SP_CLR_SIG2) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_SIG2;
				}
				if ((value & SP_SET_SIG2) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_SIG2;
				}
				if ((value & SP_CLR_SIG3) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_SIG3;
				}
				if ((value & SP_SET_SIG3) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_SIG3;
				}
				if ((value & SP_CLR_SIG4) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_SIG4;
				}
				if ((value & SP_SET_SIG4) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_SIG4;
				}
				if ((value & SP_CLR_SIG5) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_SIG5;
				}
				if ((value & SP_SET_SIG5) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_SIG5;
				}
				if ((value & SP_CLR_SIG6) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_SIG6;
				}
				if ((value & SP_SET_SIG6) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_SIG6;
				}
				if ((value & SP_CLR_SIG7) != 0)
				{
					regSP[SP_STATUS_REG] &= ~SP_STATUS_SIG7;
				}
				if ((value & SP_SET_SIG7) != 0)
				{
					regSP[SP_STATUS_REG] |= SP_STATUS_SIG7;
				}
				runRsp(dmem.getInt(0xFC0));
				break;
			case 7:
				regSP[SP_SEMAPHORE_REG] = 0;
				break;
			case 9:
				pcStackSize = value;
				break;
			case 10:
				PC[PCi] = value;
				break; // DL PC
			case 11:
				if ((value + 8) <= rdramSize)
				{
					if (PCi < (pcStackSize - 1))
					{
						PCi++;
						PC[PCi] = value;
					}
				}
				break;
			case 0x10000:
				regSP[SP_PC_REG] = value & 0xFFC;
				break;
			}
		}
	}

	@Override
	public void write64bit(int pAddr, long value)
	{
		if ((value >>> 63) != 0)
		{
			gSPLoadUcodeEx((int) value, pAddr, (short) (value >> 32));
		}
		else
		{
			while (!halt)
			{
				if ((int) value > 0 && (PC[PCi] - pAddr) >= ((int) value << 3))
				{
					break;
				}

				int w0 = rdram.read32bit(PC[PCi]);
				int w1 = rdram.read32bit(PC[PCi] + 4);

				PC[PCi] += 8;

				dUcode[current.type].write64bit(0, ((long) w0 << 32) | (w1 & 0xFFFFFFFFL));
			}
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void gSPLoadUcodeEx(int uc_start, int uc_dstart, short uc_dsize)
	{
		PCi = 0;

		if ((((uc_start & 0x1FFFFFFF) + 4096) > rdramSize) || (((uc_dstart & 0x1FFFFFFF) + uc_dsize) > rdramSize))
			return;

		Microcode ucode = detectMicrocode(uc_start, uc_dstart, uc_dsize);

		if (ucode.type != Microcode.NONE)
		{
			if (ucode != top)
			{
				if (ucode == bottom)
				{
					bottom = ucode.higher;
					bottom.lower = null;
				}
				else
				{
					ucode.higher.lower = ucode.lower;
					ucode.lower.higher = ucode.higher;
				}

				ucode.higher = null;
				ucode.lower = top;
				top.higher = ucode;
				top = ucode;
			}
			if (current == null || (current.type != ucode.type))
			{
				((Clockable) dUcode[12]).clock(1);
				((Clockable) dUcode[ucode.type]).clock(1);
			}
			current = ucode;
		}
	}

	private void runRsp(int taskType)
	{
		if ((regSP[SP_STATUS_REG] & SP_STATUS_HALT) == 0)
		{
			if ((regSP[SP_STATUS_REG] & SP_STATUS_BROKE) == 0)
			{

				if (DLIST)
				{
					if (taskType == 1)
					{
						processDList();
						regSP[SP_STATUS_REG] |= 0x0203;
						if ((regSP[SP_STATUS_REG] & SP_STATUS_INTR_BREAK) != 0)
						{
							mi.write32bit(MI_INTR_REG, MI_INTR_SET_SP);
						}
						dp.write32bit(DPC_STATUS_REG, DPC_CLR_FREEZE);
						return;
					}
				}
				if (ALIST)
				{
					if (taskType == 2)
					{
						processAList();
						regSP[SP_STATUS_REG] |= 0x0203;
						if ((regSP[SP_STATUS_REG] & SP_STATUS_INTR_BREAK) != 0)
						{
							mi.write32bit(MI_INTR_REG, MI_INTR_SET_SP);
						}
						return;
					}
				}
				if (DLIST && ALIST)
				{
					regSP[SP_STATUS_REG] |= 0x0203;
					if ((regSP[SP_STATUS_REG] & SP_STATUS_INTR_BREAK) != 0)
					{
						mi.write32bit(MI_INTR_REG, MI_INTR_SET_SP);
					}
					return;
				}
			}
		}
	}

	private void spDmaRead(int dramAddr, int memAddr, int rdLen)
	{
		dramAddr &= 0x1FFFFFFF;
		if (dramAddr > rdram.read32bit(RDRAM_CAPACITY_REG))
		{
			System.err.printf("SP DMA READ\nSP_DRAM_ADDR_REG not in RDRam space\n");
			regSP[SP_DMA_BUSY_REG] = 0;
			regSP[SP_STATUS_REG] &= ~SP_STATUS_DMA_BUSY;
			return;
		}

		if (rdLen + 1 + (memAddr & 0xFFF) > 0x1000)
		{
			System.err.printf("SP DMA READ\ncould not fit copy in memory segement\n");
			return;
		}

		((BusDMA) rdram).readDMA(dramAddr, dmem, memAddr & 0x1FFF, rdLen + 1);

		regSP[SP_DMA_BUSY_REG] = 0;
		regSP[SP_STATUS_REG] &= ~SP_STATUS_DMA_BUSY;
	}

	private void processDList()
	{
		loadGfxUcode(0, 0);

		PCi = 0;
		PC[PCi] = dmem.getInt(0x0FF0);

		halt = false;

		dp.write32bit(DPC_INIT_REG, 1);

		write64bit(0, 0L);

		((Clockable) dp).clock(1);
	}

	private void processAList()
	{
		UCData = dmem.getInt(0xFD8);
		int ucode = audio_ucode_detect();
		((Clockable) aUcode[ucode]).clock(1);

		PC[0] = dmem.getInt(0xFF0); // address
		PCi = 0;

		int listLen = dmem.getInt(0xFF4) >>> 2;

		for (int x = 0; x < listLen; x += 2)
		{
			inst1 = rdram.read32bit(PC[PCi]);
			inst2 = rdram.read32bit(PC[PCi] + 4);

			PC[PCi] += 8;

			aUcode[ucode].write64bit(0, ((long) inst1 << 32) | (inst2 & 0xFFFFFFFFL));
		}
	}

	private void loadGfxUcode(int uc_start, int uc_dstart)
	{
		((Bus32bit) dUcode[0]).write32bit(0, dmem.getInt(0x0FE4));

		int uc_start_t = dmem.getInt(0x0FD0);
		int uc_dstart_t = dmem.getInt(0x0FD8);
		int uc_dsize_t = dmem.getInt(0x0FDC);

		if ((uc_start_t != uc_start) || (uc_dstart_t != uc_dstart))
			gSPLoadUcodeEx(uc_start_t, uc_dstart_t, (short) uc_dsize_t);
	}

	private int audio_ucode_detect()
	{
		if (rdram.read32bit(UCData + 0) != 0x1)
		{
			if ((((Bus8bit) rdram).read8bit(UCData + 3) & 0xFF) == 0xF)
			{
				return 3;
			}
			else
			{
				return 2;
			}
		}
		else
		{
			if (rdram.read32bit(UCData + 0x30) == 0xF0000F00)
			{
				return 0;
			}
			else
			{
				return 1;
			}
		}
	}

	private Microcode detectMicrocode(int uc_start, int uc_dstart, short uc_dsize)
	{
		Microcode detected;

		for (int i = 0; i < numMicrocodes; i++)
		{
			detected = top;

			while (detected != null)
			{
				if ((detected.address == uc_start) && (detected.dataAddress == uc_dstart) && (detected.dataSize == uc_dsize))
					return detected;

				detected = detected.lower;
			}
		}

		detected = addMicrocode();

		detected.address = uc_start;
		detected.dataAddress = uc_dstart;
		detected.dataSize = uc_dsize;
		detected.NoN = false;
		detected.type = Microcode.NONE;

		// See if we can identify it by CRC
		crc32.reset();
		((BusDMA) rdram).readDMA(uc_start & 0x1FFFFFFF, rdramcrc, 0, 4096);
		crc32.update(rdramcrc.array(), 0, 4096);
		uc_crc = (int) crc32.getValue();
		for (int i = 0; i < specialMicrocodes.length; i++)
		{
			if (uc_crc == specialMicrocodes[i].crc)
			{
				detected.type = specialMicrocodes[i].type;
				return detected;
			}
		}

		// See if we can identify it by text
		ByteBuffer buff = ByteBuffer.allocate(2048);
		byte[] uc_data = buff.array();
		((BusDMA) rdram).readDMA(uc_dstart & 0x1FFFFFFF, buff, 0, 2048);
		uc_str = "Not Found";

		for (int i = 0; i < 2048; i++)
		{
			if ((uc_data[i] == 'R') && (uc_data[i + 1] == 'S') && (uc_data[i + 2] == 'P'))
			{
				int j = 0;
				uc_str = "";
				while (uc_data[i + j] > 0x0A)
				{
					uc_str += (char) uc_data[i + j];
					j++;
				}

				int type = Microcode.NONE;

				if ((uc_data[i + 4] == 'S') && (uc_data[i + 5] == 'W'))
				{
					type = Microcode.F3D;
				}
				else if ((uc_data[i + 4] == 'G') && (uc_data[i + 5] == 'f') && (uc_data[i + 6] == 'x'))
				{
					detected.NoN = uc_str.contains(".NoN");

					if ((uc_data[i + 14] == 'F') && (uc_data[i + 15] == '3') && (uc_data[i + 16] == 'D'))
					{
						if (detected.lower != null && detected.lower.type == Microcode.F3DEXBG)
							type = Microcode.F3DEXBG;
						else if ((uc_data[i + 19] == 'B') && (uc_data[i + 20] == 'G'))
							type = Microcode.F3DEXBG;
						else if ((uc_data[i + 28] == '0') || (uc_data[i + 28] == '1'))
							type = Microcode.F3DEX;
						else if ((uc_data[i + 31] == '2'))
							type = Microcode.F3DEX2;
					}
					else if ((uc_data[i + 14] == 'L') && (uc_data[i + 15] == '3') && (uc_data[i + 16] == 'D'))
					{
						if ((uc_data[i + 28] == '1'))
							type = Microcode.L3DEX;
						else if ((uc_data[i + 31] == '2'))
							type = Microcode.L3DEX2;
					}
					else if ((uc_data[i + 14] == 'S') && (uc_data[i + 15] == '2') && (uc_data[i + 16] == 'D'))
					{
						if ((uc_data[i + 28] == '1'))
							type = Microcode.S2DEX;
						else if ((uc_data[i + 31] == '2'))
							type = Microcode.S2DEX2;
					}
				}

				if (type != Microcode.NONE)
				{
					detected.type = type;
					return detected;
				}

				break;
			}
		}

		for (int i = 0; i < specialMicrocodes.length; i++)
		{
			if (uc_str.compareTo(specialMicrocodes[i].text) == 0)
			{
				detected.type = specialMicrocodes[i].type;
				return detected;
			}
		}

		// Let the user choose the microcode
		// String chosen = (String)JOptionPane.showInputDialog(null, uc_str, "Choose the Microcode", JOptionPane.QUESTION_MESSAGE, null, Microcode.MicrocodeTypes, Microcode.MicrocodeTypes[0]);
		// int index = Arrays.asList(Microcode.MicrocodeTypes).indexOf(chosen);
		// current.type = (index>=0)?index:Microcode.F3D;
		// System.out.printf("Couldn't find the microcode. Using %s\n", Microcode.MicrocodeTypes[current.type]);

		return detected;
	}

	private Microcode addMicrocode()
	{
		Microcode newtop = new Microcode(0, false, 0, null);

		newtop.lower = top;
		newtop.higher = null;

		if (top != null)
			top.higher = newtop;

		if (bottom == null)
			bottom = newtop;

		top = newtop;

		numMicrocodes++;

		return newtop;
	}
}
