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
import jario.hardware.Hardware;

import java.util.HashMap;

/**
 * 0x00000000 - 0x007FFFFF: RDRAM. Address-0x00000000. loadByte, loadHWord, loadWord/DWord (rdram)
 * 0x00800000 - 0x03EFFFFF: unused
 * 0x03F00000 - 0x03FFFFFF: RD REGS. Address-0x03F00000. loadWord/Dword (rdram)
 * 
 * 0x04000000 - 0x04000FFF: SP DMEM. Address-0x04000000. loadByte, loadHWord, loadWord/DWord (sp)
 * 0x04001000 - 0x04001FFF: SP IMEM. Address-0x04000000. loadByte, loadHWord, loadWord/DWord (sp)
 * 0x04040000 - 0x0407FFFF: SP REGS. Address-0x04040000. loadWord/DWord (sp) reg1
 * 0x04080000 - 0x040FFFFF: SP REGS. Address-0x04040000. loadWord/DWord (sp) reg2
 * 
 * 0x04100000 - 0x042FFFFF: DP REGS. Address-0x04100000. loadWord/DWord (dp)
 * 
 * 0x04300000 - 0x043FFFFF: MI REGS. Address-0x04300000. loadWord/DWord (mi)
 * 
 * 0x04400000 - 0x044FFFFF: VI REGS. Address-0x04400000. loadWord/DWord (vi)
 * 
 * 0x04500000 - 0x045FFFFF: AI REGS. Address-0x04500000. loadWord/DWord (ai)
 * 
 * 0x04600000 - 0x046FFFFF: PI REGS. Address-0x04600000. loadWord/DWord (pi)
 * 
 * 0x04700000 - 0x047FFFFF: RI REGS. Address-0x04700000. loadWord/DWord (ri)
 * 
 * 0x04800000 - 0x048FFFFF: SI REGS. Address-0x04800000. loadWord/DWord (si)
 * 
 * 0x05000000 - 0x07FFFFFF: CARTROM. Address-0x05000000. loadWord/DWord (cart) D1,2 A1
 * 0x08000000 - 0x0AFFFFFF: CARTROM. Address-0x05000000. loadWord/DWord (cart) D2 A2 flash
 * 0x10000000 - 0x15FFFFFF: CARTROM. Address-0x05000000. loadByte, loadWord/DWord (cart) D1 A2 rom
 * 
 * 0x1FC00000 - 0x1FC007BF: PIF ROM. Address-0x1FC00000. loadWord (pif)
 * 0x1FC007C0 - 0x1FC007FF: PIF RAM. Address-0x1FC00000. loadWord/DWord (pif)
 */
public class MIPSInterface implements Hardware, Bus8bit, Bus16bit, Bus32bit, Bus64bit
{
	private static final boolean DEBUG_MEMORY = false;

	// MIPS Interface (MI) Registers
	// private static final int MI_INIT_MODE_REG = 0;
	private static final int MI_MODE_REG = 0;
	private static final int MI_VERSION_REG = 1;
	// private static final int MI_NOOP_REG = 1;
	private static final int MI_INTR_REG = 2;
	private static final int MI_INTR_MASK_REG = 3;

	private static final int MI_CLR_INIT = 0x0080; /* Bit 7: clear init mode */
	private static final int MI_SET_INIT = 0x0100; /* Bit 8: set init mode */
	private static final int MI_CLR_EBUS = 0x0200; /* Bit 9: clear ebus test */
	private static final int MI_SET_EBUS = 0x0400; /* Bit 10: set ebus test mode */
	private static final int MI_CLR_DP_INTR = 0x0800; /* Bit 11: clear dp interrupt */
	private static final int MI_CLR_RDRAM = 0x1000; /* Bit 12: clear RDRAM reg */
	private static final int MI_SET_RDRAM = 0x2000; /* Bit 13: set RDRAM reg mode */

	private static final int MI_MODE_INIT = 0x0080; /* Bit 7: init mode */
	private static final int MI_MODE_EBUS = 0x0100; /* Bit 8: ebus test mode */
	private static final int MI_MODE_RDRAM = 0x0200; /* Bit 9: RDRAM reg mode */

	private static final int MI_INTR_CLR_SP = 0x0001; /* Bit 0: clear SP interrupt */
	private static final int MI_INTR_SET_SP = 0x0002; /* Bit 1: set SP interrupt */
	private static final int MI_INTR_CLR_SI = 0x0004; /* Bit 2: clear SI interrupt */
	private static final int MI_INTR_SET_SI = 0x0008; /* Bit 3: set SI interrupt */
	private static final int MI_INTR_CLR_AI = 0x0010; /* Bit 4: clear AI interrupt */
	private static final int MI_INTR_SET_AI = 0x0020; /* Bit 5: set AI interrupt */
	private static final int MI_INTR_CLR_VI = 0x0040; /* Bit 6: clear VI interrupt */
	private static final int MI_INTR_SET_VI = 0x0080; /* Bit 7: set VI interrupt */
	private static final int MI_INTR_CLR_PI = 0x0100; /* Bit 8: clear PI interrupt */
	private static final int MI_INTR_SET_PI = 0x0200; /* Bit 9: set PI interrupt */
	private static final int MI_INTR_CLR_DP = 0x0400; /* Bit 10: clear DP interrupt */
	private static final int MI_INTR_SET_DP = 0x0800; /* Bit 11: set DP interrupt */

	private static final int MI_INTR_MASK_CLR_SP = 0x0001; /* Bit 0: clear SP mask */
	private static final int MI_INTR_MASK_SET_SP = 0x0002; /* Bit 1: set SP mask */
	private static final int MI_INTR_MASK_CLR_SI = 0x0004; /* Bit 2: clear SI mask */
	private static final int MI_INTR_MASK_SET_SI = 0x0008; /* Bit 3: set SI mask */
	private static final int MI_INTR_MASK_CLR_AI = 0x0010; /* Bit 4: clear AI mask */
	private static final int MI_INTR_MASK_SET_AI = 0x0020; /* Bit 5: set AI mask */
	private static final int MI_INTR_MASK_CLR_VI = 0x0040; /* Bit 6: clear VI mask */
	private static final int MI_INTR_MASK_SET_VI = 0x0080; /* Bit 7: set VI mask */
	private static final int MI_INTR_MASK_CLR_PI = 0x0100; /* Bit 8: clear PI mask */
	private static final int MI_INTR_MASK_SET_PI = 0x0200; /* Bit 9: set PI mask */
	private static final int MI_INTR_MASK_CLR_DP = 0x0400; /* Bit 10: clear DP mask */
	private static final int MI_INTR_MASK_SET_DP = 0x0800; /* Bit 11: set DP mask */

	private static final int MI_INTR_MASK_SP = 0x01; /* Bit 0: SP intr mask */
	private static final int MI_INTR_MASK_SI = 0x02; /* Bit 1: SI intr mask */
	private static final int MI_INTR_MASK_AI = 0x04; /* Bit 2: AI intr mask */
	private static final int MI_INTR_MASK_VI = 0x08; /* Bit 3: VI intr mask */
	private static final int MI_INTR_MASK_PI = 0x10; /* Bit 4: PI intr mask */
	private static final int MI_INTR_MASK_DP = 0x20; /* Bit 5: DP intr mask */

	private static final int MI_INTR_SP = 0x01; /* Bit 0: SP intr */
	private static final int MI_INTR_SI = 0x02; /* Bit 1: SI intr */
	private static final int MI_INTR_AI = 0x04; /* Bit 2: AI intr */
	private static final int MI_INTR_VI = 0x08; /* Bit 3: VI intr */
	private static final int MI_INTR_PI = 0x10; /* Bit 4: PI intr */
	private static final int MI_INTR_DP = 0x20; /* Bit 5: DP intr */

	private int[] regMI = new int[4];

	private Bus8bit sp8bit;
	private Bus16bit sp16bit;
	private Bus32bit sp32bit;
	private Bus32bit dp;
	private Bus32bit cpu;
	private Bus32bit vi;
	private Bus32bit ai;
	private Bus32bit pi;
	private Bus32bit ri;
	private Bus32bit si;
	private Bus32bit cart;
	private Bus8bit rdram8bit;
	private Bus16bit rdram16bit;
	private Bus32bit rdram32bit;
	private Bus32bit pif;

	private HashMap<Integer, Byte> bMem = new HashMap<Integer, Byte>();

	public MIPSInterface()
	{
		regMI[MI_VERSION_REG] = 0x02020102;
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			rdram8bit = (Bus8bit) bus;
			rdram16bit = (Bus16bit) bus;
			rdram32bit = (Bus32bit) bus;
			break;
		case 1:
			sp8bit = (Bus8bit) bus;
			sp16bit = (Bus16bit) bus;
			sp32bit = (Bus32bit) bus;
			break;
		case 2:
			dp = (Bus32bit) bus;
			break;
		case 3:
			cpu = (Bus32bit) bus;
			break;
		case 4:
			vi = (Bus32bit) bus;
			break;
		case 5:
			ai = (Bus32bit) bus;
			break;
		case 6:
			pi = (Bus32bit) bus;
			break;
		case 7:
			ri = (Bus32bit) bus;
			break;
		case 8:
			si = (Bus32bit) bus;
			break;
		case 9:
			cart = (Bus32bit) bus;
			break;
		case 10:
			pif = (Bus32bit) bus;
			break;
		default:
			System.err.println("Attempting to connect bus on invalid port: " + port);
			break;
		}
	}

	@Override
	public void reset()
	{
		regMI[MI_VERSION_REG] = 0x02020102;
	}

	@Override
	public final byte read8bit(int pAddr)
	{
		if (DEBUG_MEMORY)
			System.out.printf("LB:%X ", pAddr);

		if (pAddr >= 0x00000000 && pAddr < 0x00800000)
		{ // RAM = 0x00000000 to 0x003FFFFF, Extended RAM = 0x00400000 to 0x007FFFFF
			return rdram8bit.read8bit(pAddr);
		}
		else if (pAddr >= 0x00800000 && pAddr < 0x03F00000)
		{ // ?? Unused
			System.err.println("Illegal Memory LB access: " + Integer.toHexString(pAddr));
			return 0;
		}
		else if (pAddr >= 0x04000000 && pAddr < 0x04002000)
		{ // DMEM/IMEM
			return sp8bit.read8bit(pAddr - 0x04000000);
		}
		else
		{
			if (pAddr >= 0x10000000 && pAddr < 0x16000000) { return ((Bus8bit) cart).read8bit(pAddr - 0x05000000); }
			switch (pAddr & 0xFFF00000)
			{
			default:
				Byte b = bMem.get(pAddr);
				return ((b == null) ? 0 : b);
			}
		}
	}

	@Override
	public final short read16bit(int pAddr)
	{
		if (DEBUG_MEMORY)
			System.out.printf("LH:%X ", pAddr);

		if (pAddr >= 0x00000000 && pAddr < 0x00800000)
		{ // RAM = 0x00000000 to 0x003FFFFF, Extended RAM = 0x00400000 to 0x007FFFFF
			return rdram16bit.read16bit(pAddr);
		}
		else if (pAddr >= 0x00800000 && pAddr < 0x03F00000)
		{ // ?? Unused
			System.err.println("Illegal Memory LH access: " + Integer.toHexString(pAddr));
			return 0;
		}
		else if (pAddr >= 0x04000000 && pAddr < 0x04002000)
		{ // DMEM/IMEM
			return sp16bit.read16bit(pAddr - 0x04000000);
		}
		else
		{
			switch (pAddr & 0xFFF00000)
			{
			default:
				Byte bb1 = bMem.get(pAddr);
				int b1 = ((bb1 == null) ? 0 : bb1) & 0xFF;
				Byte bb2 = bMem.get(pAddr + 1);
				int b2 = ((bb2 == null) ? 0 : bb2) & 0xFF;
				return (short) ((b1 << 8) | b2);
			}
		}
	}

	@Override
	public final int read32bit(int pAddr)
	{
		if (DEBUG_MEMORY)
			System.out.printf("LW:%X ", pAddr);

		if (pAddr >= 0x00000000 && pAddr < 0x00800000)
		{ // RAM = 0x00000000 to 0x003FFFFF, Extended RAM = 0x00400000 to 0x007FFFFF
			return rdram32bit.read32bit(pAddr);
		}
		else if (pAddr >= 0x00800000 && pAddr < 0x03F00000)
		{ // ?? Unused
			System.err.println("Illegal Memory LW access: " + Integer.toHexString(pAddr));
			return 0;
		}
		else if (pAddr >= 0x04000000 && pAddr < 0x04002000)
		{ // DMEM/IMEM
			return sp32bit.read32bit(pAddr - 0x04000000);
		}
		else
		{
			if (pAddr >= 0x10000000 && pAddr < 0x16000000) { return cart.read32bit(pAddr - 0x05000000); }

			switch (pAddr & 0xFFF00000)
			{
			case 0x03F00000:
				return rdram32bit.read32bit(pAddr);
			case 0x04000000:
				return sp32bit.read32bit(pAddr);
			case 0x04100000:
				return dp.read32bit(pAddr);
			case 0x04300000:
				return readRegister((pAddr - 0x04300000) >> 2);
			case 0x04400000:
				return vi.read32bit(pAddr);
			case 0x04500000:
				return ai.read32bit(pAddr);
			case 0x04600000:
				return pi.read32bit(pAddr);
			case 0x04700000:
				return ri.read32bit(pAddr);
			case 0x04800000:
				return si.read32bit(pAddr);
			case 0x05000000:
				return cart.read32bit(pAddr - 0x05000000);
			case 0x08000000:
				return cart.read32bit(pAddr - 0x05000000);
			case 0x1FC00000:
				return pif.read32bit(pAddr - 0x1FC00000);
			default:
				Byte bb1 = bMem.get(pAddr);
				int b1 = ((bb1 == null) ? 0 : bb1) & 0xFF;
				Byte bb2 = bMem.get(pAddr + 1);
				int b2 = ((bb2 == null) ? 0 : bb2) & 0xFF;
				Byte bb3 = bMem.get(pAddr + 2);
				int b3 = ((bb3 == null) ? 0 : bb3) & 0xFF;
				Byte bb4 = bMem.get(pAddr + 3);
				int b4 = ((bb4 == null) ? 0 : bb4) & 0xFF;
				return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
			}
		}
	}

	@Override
	public final long read64bit(int pAddr)
	{
		if (DEBUG_MEMORY)
			System.out.printf("LD:%X ", pAddr);

		return (((long) read32bit(pAddr)) << 32) | (((long) read32bit(pAddr + 4)) & 0xFFFFFFFFL);
	}

	@Override
	public final void write8bit(int pAddr, byte value)
	{
		if (DEBUG_MEMORY)
			System.out.printf("SB:%X ", pAddr);

		if (pAddr >= 0x00000000 && pAddr < 0x00800000)
		{ // RAM = 0x00000000 to 0x003FFFFF, Extended RAM = 0x00400000 to 0x007FFFFF
			rdram8bit.write8bit(pAddr, value);
		}
		else if (pAddr >= 0x00800000 && pAddr < 0x03F00000)
		{ // ?? Unused
			System.err.println("Illegal Memory SB access: " + Integer.toHexString(pAddr));
			return;
		}
		else if (pAddr >= 0x04000000 && pAddr < 0x04002000)
		{ // DMEM/IMEM
			sp8bit.write8bit(pAddr - 0x04000000, value);
		}
		else
		{
			switch (pAddr & 0xFFF00000)
			{
			case 0x00000000:
			case 0x00100000:
			case 0x00200000:
			case 0x00300000:
			case 0x00400000:
			case 0x00500000:
			case 0x00600000:
			case 0x00700000:
				System.err.println("Illegal RAM Memory SB: " + Integer.toHexString(pAddr));
				return;
			default:
				bMem.put(pAddr, value);
			}
		}
	}

	@Override
	public final void write16bit(int pAddr, short value)
	{
		if (DEBUG_MEMORY)
			System.out.printf("SH:%X ", pAddr);

		if (pAddr >= 0x00000000 && pAddr < 0x00800000)
		{ // RAM = 0x00000000 to 0x003FFFFF, Extended RAM = 0x00400000 to 0x007FFFFF
			rdram16bit.write16bit(pAddr, value);
		}
		else if (pAddr >= 0x00800000 && pAddr < 0x03F00000)
		{ // ?? Unused
			System.err.println("Illegal Memory SH access: " + Integer.toHexString(pAddr));
			return;
		}
		else if (pAddr >= 0x04000000 && pAddr < 0x04002000)
		{ // DMEM/IMEM
			sp16bit.write16bit(pAddr - 0x04000000, value);
		}
		else
		{
			switch (pAddr & 0xFFF00000)
			{
			case 0x00000000:
			case 0x00100000:
			case 0x00200000:
			case 0x00300000:
			case 0x00400000:
			case 0x00500000:
			case 0x00600000:
			case 0x00700000:
				System.err.println("Illegal RAM Memory SH: " + Integer.toHexString(pAddr));
				return;
			default:
				bMem.put(pAddr, (byte) ((value >> 8) & 0xFFFF));
				bMem.put(pAddr + 1, (byte) ((value) & 0xFFFF));
			}
		}
	}

	@Override
	public final void write32bit(int pAddr, int value)
	{
		if (DEBUG_MEMORY)
			System.out.printf("SW:%X ", pAddr);

		if (pAddr >= 0x00000000 && pAddr < 0x00800000)
		{ // RAM = 0x00000000 to 0x003FFFFF, Extended RAM = 0x00400000 to 0x007FFFFF
			rdram32bit.write32bit(pAddr, value);
		}
		else if (pAddr >= 0x00800000 && pAddr < 0x03F00000)
		{ // ?? Unused
			System.err.println("Illegal Memory  SW access: " + Integer.toHexString(pAddr));
			return;
		}
		else if (pAddr >= 0x04000000 && pAddr < 0x04002000)
		{ // DMEM/IMEM
			sp32bit.write32bit(pAddr - 0x04000000, value);
		}
		else
		{
			if (pAddr >= 0x10000000 && pAddr < 0x16000000)
			{
				cart.write32bit(pAddr - 0x05000000, value);
			}

			switch (pAddr & 0xFFF00000)
			{
			case 0x00000000:
			case 0x00100000:
			case 0x00200000:
			case 0x00300000:
			case 0x00400000:
			case 0x00500000:
			case 0x00600000:
			case 0x00700000:
				System.err.println("Illegal RAM Memory SW: " + Integer.toHexString(pAddr));
				return;
			case 0x03F00000:
				rdram32bit.write32bit(pAddr, value);
				break;
			case 0x04000000:
				sp32bit.write32bit(pAddr, value);
				break;
			case 0x04100000:
				dp.write32bit(pAddr, value);
				break;
			case 0x04300000:
				writeRegister((pAddr - 0x04300000) >> 2, value);
				break;
			case 0x04400000:
				vi.write32bit(pAddr, value);
				break;
			case 0x04500000:
				ai.write32bit(pAddr, value);
				break;
			case 0x04600000:
				pi.write32bit(pAddr, value);
				break;
			case 0x04700000:
				ri.write32bit(pAddr, value);
				break;
			case 0x04800000:
				si.write32bit(pAddr, value);
				break;
			case 0x08000000:
				cart.write32bit(pAddr - 0x05000000, value);
				break;
			case 0x1FC00000:
				pif.write32bit(pAddr - 0x1FC00000, value);
				break;
			default:
				bMem.put(pAddr, (byte) ((value >> 24) & 0xFFFF));
				bMem.put(pAddr + 1, (byte) ((value >> 16) & 0xFFFF));
				bMem.put(pAddr + 2, (byte) ((value >> 8) & 0xFFFF));
				bMem.put(pAddr + 3, (byte) ((value) & 0xFFFF));
			}
		}
	}

	@Override
	public final void write64bit(int pAddr, long value)
	{
		if (DEBUG_MEMORY)
			System.out.printf("SD:%X ", pAddr);

		write32bit(pAddr, (int) (value >> 32));
		write32bit(pAddr + 4, (int) value);
	}

	// private methods //////////////////////////////////////////////////

	private int readRegister(int reg)
	{
		switch (reg)
		{
		case 0:
			return regMI[MI_MODE_REG];
		case 1:
			return regMI[MI_VERSION_REG];
		case 2:
			return regMI[MI_INTR_REG];
		case 3:
			return regMI[MI_INTR_MASK_REG];
		default:
			return 0;
		}
	}

	private void writeRegister(int reg, int value)
	{
		switch (reg)
		{
		case 0:
			regMI[MI_MODE_REG] &= ~0x7F;
			regMI[MI_MODE_REG] |= (value & 0x7F);
			if ((value & MI_CLR_INIT) != 0)
			{
				regMI[MI_MODE_REG] &= ~MI_MODE_INIT;
			}
			if ((value & MI_SET_INIT) != 0)
			{
				regMI[MI_MODE_REG] |= MI_MODE_INIT;
			}
			if ((value & MI_CLR_EBUS) != 0)
			{
				regMI[MI_MODE_REG] &= ~MI_MODE_EBUS;
			}
			if ((value & MI_SET_EBUS) != 0)
			{
				regMI[MI_MODE_REG] |= MI_MODE_EBUS;
			}
			if ((value & MI_CLR_DP_INTR) != 0)
			{
				regMI[MI_INTR_REG] &= ~MI_INTR_DP;
				cpu.write32bit(38, regMI[MI_INTR_MASK_REG] & regMI[MI_INTR_REG]);
			}
			if ((value & MI_CLR_RDRAM) != 0)
			{
				regMI[MI_MODE_REG] &= ~MI_MODE_RDRAM;
			}
			if ((value & MI_SET_RDRAM) != 0)
			{
				regMI[MI_MODE_REG] |= MI_MODE_RDRAM;
			}
			break;
		case 2:
			if ((value & MI_INTR_CLR_SP) != 0)
			{
				regMI[MI_INTR_REG] &= ~MI_INTR_SP;
			}
			if ((value & MI_INTR_SET_SP) != 0)
			{
				regMI[MI_INTR_REG] |= MI_INTR_SP;
			}
			if ((value & MI_INTR_CLR_SI) != 0)
			{
				regMI[MI_INTR_REG] &= ~MI_INTR_SI;
			}
			if ((value & MI_INTR_SET_SI) != 0)
			{
				regMI[MI_INTR_REG] |= MI_INTR_SI;
			}
			if ((value & MI_INTR_CLR_AI) != 0)
			{
				regMI[MI_INTR_REG] &= ~MI_INTR_AI;
			}
			if ((value & MI_INTR_SET_AI) != 0)
			{
				regMI[MI_INTR_REG] |= MI_INTR_AI;
			}
			if ((value & MI_INTR_CLR_VI) != 0)
			{
				regMI[MI_INTR_REG] &= ~MI_INTR_VI;
			}
			if ((value & MI_INTR_SET_VI) != 0)
			{
				regMI[MI_INTR_REG] |= MI_INTR_VI;
			}
			if ((value & MI_INTR_CLR_PI) != 0)
			{
				regMI[MI_INTR_REG] &= ~MI_INTR_PI;
			}
			if ((value & MI_INTR_SET_PI) != 0)
			{
				regMI[MI_INTR_REG] |= MI_INTR_PI;
			}
			if ((value & MI_INTR_CLR_DP) != 0)
			{
				regMI[MI_INTR_REG] &= ~MI_INTR_DP;
			}
			if ((value & MI_INTR_SET_DP) != 0)
			{
				regMI[MI_INTR_REG] |= MI_INTR_DP;
			}
			cpu.write32bit(38, regMI[MI_INTR_MASK_REG] & regMI[MI_INTR_REG]);
			break;
		case 3:
			if ((value & MI_INTR_MASK_CLR_SP) != 0)
			{
				regMI[MI_INTR_MASK_REG] &= ~MI_INTR_MASK_SP;
			}
			if ((value & MI_INTR_MASK_SET_SP) != 0)
			{
				regMI[MI_INTR_MASK_REG] |= MI_INTR_MASK_SP;
			}
			if ((value & MI_INTR_MASK_CLR_SI) != 0)
			{
				regMI[MI_INTR_MASK_REG] &= ~MI_INTR_MASK_SI;
			}
			if ((value & MI_INTR_MASK_SET_SI) != 0)
			{
				regMI[MI_INTR_MASK_REG] |= MI_INTR_MASK_SI;
			}
			if ((value & MI_INTR_MASK_CLR_AI) != 0)
			{
				regMI[MI_INTR_MASK_REG] &= ~MI_INTR_MASK_AI;
			}
			if ((value & MI_INTR_MASK_SET_AI) != 0)
			{
				regMI[MI_INTR_MASK_REG] |= MI_INTR_MASK_AI;
			}
			if ((value & MI_INTR_MASK_CLR_VI) != 0)
			{
				regMI[MI_INTR_MASK_REG] &= ~MI_INTR_MASK_VI;
			}
			if ((value & MI_INTR_MASK_SET_VI) != 0)
			{
				regMI[MI_INTR_MASK_REG] |= MI_INTR_MASK_VI;
			}
			if ((value & MI_INTR_MASK_CLR_PI) != 0)
			{
				regMI[MI_INTR_MASK_REG] &= ~MI_INTR_MASK_PI;
			}
			if ((value & MI_INTR_MASK_SET_PI) != 0)
			{
				regMI[MI_INTR_MASK_REG] |= MI_INTR_MASK_PI;
			}
			if ((value & MI_INTR_MASK_CLR_DP) != 0)
			{
				regMI[MI_INTR_MASK_REG] &= ~MI_INTR_MASK_DP;
			}
			if ((value & MI_INTR_MASK_SET_DP) != 0)
			{
				regMI[MI_INTR_MASK_REG] |= MI_INTR_MASK_DP;
			}
			break;
		}
	}
}
