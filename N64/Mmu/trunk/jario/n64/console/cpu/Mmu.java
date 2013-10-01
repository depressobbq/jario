/**
 * Copyright 2013 Jason LaDere
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

package jario.n64.console.cpu;

import jario.hardware.Bus1bit;
import jario.hardware.Bus32bit;
import jario.hardware.Hardware;

public class Mmu implements Hardware, Bus1bit, Bus32bit
{
	private static final boolean DEBUG_TLB = false;

	private static final int INDEX_REGISTER = 0;
	private static final int RANDOM_REGISTER = 1;
	private static final int ENTRYLO0_REGISTER = 2;
	private static final int ENTRYLO1_REGISTER = 3;
	private static final int PAGE_MASK_REGISTER = 5;
	private static final int BAD_VADDR_REGISTER = 8;
	private static final int ENTRYHI_REGISTER = 10;

	// private static final int EXC_MOD = 1<<2; /* Tlb mod */
	private static final int EXC_RMISS = 2 << 2; /* Read Tlb Miss */

	// private static final int EXC_WMISS = 3<<2; /* Write Tlb Miss */

	private class Tlb
	{
		boolean entryDefined = false;
		int pageMaskZero;
		int pageMaskMask;
		int pageMaskZero2;

		public void setPageMask(int value)
		{
			pageMaskZero2 = (value >> 25) & 0x7F;
			pageMaskMask = (value >> 13) & 0xFFF;
			pageMaskZero = (value) & 0x1FFF;
		}

		public int getPageMask()
		{
			return (pageMaskZero2 << 25) | (pageMaskMask << 13) | (pageMaskZero);
		}

		int entryHiASID;
		int entryHiZero;
		int entryHiG;
		int entryHiVPN2;

		public void setEntryHi(int value)
		{
			entryHiVPN2 = (value >> 13) & 0x7FFFF;
			entryHiG = (value >> 12) & 0x1;
			entryHiZero = (value >> 8) & 0xF;
			entryHiASID = (value) & 0xFF;
		}

		public int getEntryHi()
		{
			return (entryHiVPN2 << 13) | (entryHiG << 12) | (entryHiZero << 8) | (entryHiASID);
		}

		boolean entryLo0GLOBAL;
		boolean entryLo0V;
		boolean entryLo0D;
		int entryLo0C;
		int entryLo0PFN;
		int entryLo0ZERO;

		public void setEntryLo0(int value)
		{
			entryLo0ZERO = (value >> 26) & 0x3F;
			entryLo0PFN = (value >> 6) & 0xFFFFF;
			entryLo0C = (value >> 3) & 0x7;
			entryLo0D = ((value >> 2) & 0x1) == 1 ? true : false;
			entryLo0V = ((value >> 1) & 0x1) == 1 ? true : false;
			entryLo0GLOBAL = ((value) & 0x1) == 1 ? true : false;
		}

		public int getEntryLo0()
		{
			return (entryLo0ZERO << 26) | (entryLo0PFN << 6) | (entryLo0C << 3) | ((entryLo0D ? 1 : 0) << 2) | ((entryLo0V ? 1 : 0) << 1) | (entryLo0GLOBAL ? 1 : 0);
		}

		boolean entryLo1GLOBAL;
		boolean entryLo1V;
		boolean entryLo1D;
		int entryLo1C;
		int entryLo1PFN;
		int entryLo1ZERO;

		public void setEntryLo1(int value)
		{
			entryLo1ZERO = (value >> 26) & 0x3F;
			entryLo1PFN = (value >> 6) & 0xFFFFF;
			entryLo1C = (value >> 3) & 0x7;
			entryLo1D = ((value >> 2) & 0x1) == 1 ? true : false;
			entryLo1V = ((value >> 1) & 0x1) == 1 ? true : false;
			entryLo1GLOBAL = ((value) & 0x1) == 1 ? true : false;
		}

		public int getEntryLo1()
		{
			return (entryLo1ZERO << 26) | (entryLo1PFN << 6) | (entryLo1C << 3) | ((entryLo1D ? 1 : 0) << 2) | ((entryLo1V ? 1 : 0) << 1) | (entryLo1GLOBAL ? 1 : 0);
		}
	};

	private class FastTlb
	{
		long vStart;
		long vEnd;
		long physStart;
		boolean valid;
		boolean dirty;
		// boolean global;
		boolean validEntry = false;
	};

	public static interface OpCode
	{
		public void exec(int inst1, int inst2);
	}

	private FastTlb[] fastTlb = new FastTlb[64];
	private Tlb[] tlb = new Tlb[32];
	private int[] tlbReadMap;
	private int[] tlbWriteMap;
	private OpCode[] r4300i_Tlb_Function;
	private boolean useTlb = true;
	private boolean showTLBMisses = false;

	private Bus32bit cp0;

	public Mmu()
	{
		tlbReadMap = new int[0xFFFFF]; // 1048575, 4,194,300b (4MB)
		tlbWriteMap = new int[0xFFFFF]; // 1048575, 4,194,300b (4MB)
		for (int count = 0; count < 32; count++)
		{
			tlb[count] = new Tlb();
		}
		for (int count = 0; count < 64; count++)
		{
			fastTlb[count] = new FastTlb();
		}
		setupTlb();
		buildOps();
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			cp0 = (Bus32bit) bus;
			break;
		}
	}

	@Override
	public void reset()
	{
		fastTlb = new FastTlb[64];
		tlb = new Tlb[32];
		tlbReadMap = new int[0xFFFFF]; // 1048575, 4,194,300b (4MB)
		tlbWriteMap = new int[0xFFFFF]; // 1048575, 4,194,300b (4MB)
		for (int count = 0; count < 32; count++)
		{
			tlb[count] = new Tlb();
		}
		for (int count = 0; count < 64; count++)
		{
			fastTlb[count] = new FastTlb();
		}
		setupTlb();
	}

	@Override
	public boolean read1bit(int pAddr)
	{
		return addressDefined() != 0;
	}

	@Override
	public final int read32bit(int pAddr)
	{
		if (!useTlb)
		{
			return pAddr & 0x1FFFFFFF;
		}

		if (tlbReadMap[pAddr >>> 12] == 0)
		{
			cp0.write32bit(33, pAddr);
			cp0.write32bit(42, EXC_RMISS);
			if (showTLBMisses)
			{
				Thread.dumpStack();
				System.err.printf("Tlb miss address: %X\n", pAddr);
			}
			return pAddr & 0x1FFFFFFF;
		}
		return tlbReadMap[pAddr >>> 12] + pAddr;
	}

	@Override
	public void write1bit(int pAddr, boolean value)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void write32bit(int pAddr, int value)
	{
		r4300i_Tlb_Function[value & 0x3F].exec(value, 0);
	}

	// Private Methods ////////////////////////////////////////////////////////

	private void buildOps()
	{
		r4300i_Tlb_Function = new OpCode[16];
		for (int i = 0; i < 16; i++)
		{
			r4300i_Tlb_Function[i] = R4300i_UnknownOpcode;
		}
		r4300i_Tlb_Function[1] = r4300i_COP0_CO_TLBR;
		r4300i_Tlb_Function[2] = r4300i_COP0_CO_TLBWI;
		r4300i_Tlb_Function[6] = r4300i_COP0_CO_TLBWR;
		r4300i_Tlb_Function[8] = r4300i_COP0_CO_TLBP;
	}

	private int addressDefined()
	{
		long address = cp0.read32bit(BAD_VADDR_REGISTER) & 0xFFFFFFFFL;
		if (address >= 0x80000000L && address <= 0xBFFFFFFFL)
		{
			return 1;
		}
		for (int i = 0; i < 64; i++)
		{
			if (!fastTlb[i].validEntry)
			{
				continue;
			}
			if (address >= fastTlb[i].vStart && address <= fastTlb[i].vEnd)
			{
				return 1;
			}
		}
		return 0;
	}

	private void tlbProbe()
	{
		if (DEBUG_TLB)
		{
			System.out.printf("TLBP:%X\n", cp0.read32bit(ENTRYHI_REGISTER));
		}
		
		cp0.write32bit(INDEX_REGISTER, cp0.read32bit(INDEX_REGISTER) | 0x80000000);
		
		for (int count = 0; count < 32; count++)
		{
			int tlbValue = tlb[count].getEntryHi() & (~tlb[count].pageMaskMask << 13);
			int entryHi = cp0.read32bit(ENTRYHI_REGISTER) & (~tlb[count].pageMaskMask << 13);

			if (tlbValue == entryHi)
			{
				boolean global = (tlb[count].getEntryHi() & 0x100) != 0;
				boolean sameAsid = ((tlb[count].getEntryHi() & 0xFF) == (cp0.read32bit(ENTRYHI_REGISTER) & 0xFF));

				if (global || sameAsid)
				{
					cp0.write32bit(INDEX_REGISTER, count);
					return;
				}
			}
		}

		if (DEBUG_TLB)
		{
			System.out.printf("0=%X\n", cp0.read32bit(INDEX_REGISTER));
		}
	}

	private void tlbRead()
	{
		if (DEBUG_TLB)
		{
			System.out.printf("TLBR:%X\n", cp0.read32bit(INDEX_REGISTER));
		}
		
		int index = cp0.read32bit(INDEX_REGISTER) & 0x1F;

		cp0.write32bit(PAGE_MASK_REGISTER, tlb[index].getPageMask());
		cp0.write32bit(ENTRYHI_REGISTER, (tlb[index].getEntryHi() & ~tlb[index].getPageMask()));
		cp0.write32bit(ENTRYLO0_REGISTER, tlb[index].getEntryLo0());
		cp0.write32bit(ENTRYLO1_REGISTER, tlb[index].getEntryLo1());

		if (DEBUG_TLB)
		{
			System.out.printf("5=%X 10=%X 2=%X 3=%X\n", cp0.read32bit(PAGE_MASK_REGISTER), cp0.read32bit(ENTRYHI_REGISTER), cp0.read32bit(ENTRYLO0_REGISTER), cp0.read32bit(ENTRYLO1_REGISTER));
		}
	}

	private void writeTlbEntry(int index)
	{
		if (tlb[index].entryDefined)
		{
			for (int fastIndx = index << 1; fastIndx <= (index << 1) + 1; fastIndx++)
			{
				if (!fastTlb[fastIndx].validEntry)
				{
					continue;
				}
				if (!fastTlb[fastIndx].valid)
				{
					continue;
				}
				for (long vAddr = fastTlb[fastIndx].vStart; vAddr < fastTlb[fastIndx].vEnd; vAddr += 0x1000L)
				{
					tlbReadMap[(int) (vAddr >> 12)] = 0;
					tlbWriteMap[(int) (vAddr >> 12)] = 0;
				}
			}
		}
		tlb[index].setPageMask(cp0.read32bit(PAGE_MASK_REGISTER));
		tlb[index].setEntryHi(cp0.read32bit(ENTRYHI_REGISTER));
		tlb[index].setEntryLo0(cp0.read32bit(ENTRYLO0_REGISTER));
		tlb[index].setEntryLo1(cp0.read32bit(ENTRYLO1_REGISTER));
		tlb[index].entryDefined = true;

		setupTlbEntry(index);
	}

	private void setupTlb()
	{
		for (int i = 0; i < tlbReadMap.length; i++)
		{
			tlbReadMap[i] = 0;
		}
		for (int i = 0; i < tlbWriteMap.length; i++)
		{
			tlbWriteMap[i] = 0;
		}
		for (long vAddr = 0x80000000L; vAddr < 0xC0000000L; vAddr += 0x1000L)
		{
			tlbReadMap[(int) (vAddr >> 12)] = (int) ((vAddr & 0x1FFFFFFFL) - vAddr);
			tlbWriteMap[(int) (vAddr >> 12)] = (int) ((vAddr & 0x1FFFFFFFL) - vAddr);
		}
		for (int count = 0; count < 32; count++)
		{
			setupTlbEntry(count);
		}
	}

	private void setupTlbEntry(int entry)
	{
		if (!tlb[entry].entryDefined)
		{
			return;
		}

		int fastIndx = entry << 1;
		fastTlb[fastIndx].vStart = ((long) tlb[entry].entryHiVPN2) << 13;
		fastTlb[fastIndx].vEnd = fastTlb[fastIndx].vStart + (tlb[entry].pageMaskMask << 12) + 0xFFF;
		fastTlb[fastIndx].physStart = ((long) tlb[entry].entryLo0PFN) << 12;
		fastTlb[fastIndx].valid = tlb[entry].entryLo0V;
		fastTlb[fastIndx].dirty = tlb[entry].entryLo0D;
		// fastTlb[fastIndx].global = tlb[entry].entryLo0GLOBAL & tlb[entry].entryLo1GLOBAL;
		fastTlb[fastIndx].validEntry = false;

		fastIndx = (entry << 1) + 1;
		fastTlb[fastIndx].vStart = (((long) tlb[entry].entryHiVPN2) << 13) + ((((long) tlb[entry].pageMaskMask) << 12) + 0xFFF + 1);
		fastTlb[fastIndx].vEnd = fastTlb[fastIndx].vStart + (((long) tlb[entry].pageMaskMask) << 12) + 0xFFF;
		fastTlb[fastIndx].physStart = ((long) tlb[entry].entryLo1PFN) << 12;
		fastTlb[fastIndx].valid = tlb[entry].entryLo1V;
		fastTlb[fastIndx].dirty = tlb[entry].entryLo1D;
		// fastTlb[fastIndx].global = tlb[entry].entryLo0GLOBAL & tlb[entry].entryLo1GLOBAL;
		fastTlb[fastIndx].validEntry = false;

		for (fastIndx = entry << 1; fastIndx <= (entry << 1) + 1; fastIndx++)
		{
			if (!fastTlb[fastIndx].valid)
			{
				fastTlb[fastIndx].validEntry = true;
				continue;
			}
			if (fastTlb[fastIndx].vEnd <= fastTlb[fastIndx].vStart)
			{
				System.err.printf("Vstart = Vend for tlb mapping\n");
				continue;
			}
			if (fastTlb[fastIndx].vStart >= 0x80000000L && fastTlb[fastIndx].vEnd <= 0xBFFFFFFFL)
			{
				continue;
			}
			if (fastTlb[fastIndx].physStart > 0x1FFFFFFFL)
			{
				continue;
			}

			// test if overlap
			fastTlb[fastIndx].validEntry = true;
			for (long vAddr = fastTlb[fastIndx].vStart; vAddr < fastTlb[fastIndx].vEnd; vAddr += 0x1000L)
			{
				tlbReadMap[(int) (vAddr >> 12)] = (int) ((vAddr - fastTlb[fastIndx].vStart + fastTlb[fastIndx].physStart) - vAddr);
				if (!fastTlb[fastIndx].dirty)
				{
					continue;
				}
				tlbWriteMap[(int) (vAddr >> 12)] = (int) ((vAddr - fastTlb[fastIndx].vStart + fastTlb[fastIndx].physStart) - vAddr);
			}
		}
	}

	protected OpCode r4300i_COP0_CO_TLBR = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			tlbRead();
		}
	};

	protected OpCode r4300i_COP0_CO_TLBWI = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			writeTlbEntry(cp0.read32bit(INDEX_REGISTER) & 0x1F);
		}
	};

	protected OpCode r4300i_COP0_CO_TLBWR = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			writeTlbEntry(cp0.read32bit(RANDOM_REGISTER) & 0x1F);
		}
	};

	protected OpCode r4300i_COP0_CO_TLBP = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			tlbProbe();
		}
	};

	protected OpCode R4300i_UnknownOpcode = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			System.err.printf("Unhandled r4300i Cop0 OpCode:%X\n", inst);
			System.exit(0);
		}
	};
}
