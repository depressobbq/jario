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

package jario.n64.console.cpu;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import jario.hardware.Bus1bit;
import jario.hardware.Bus32bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

/**
 * Registers:
 * 0 - 31: (r/w) System Control Coprocessor 0 Registers (32 bit)
 * 33: (r/w) ADDRESS_REG - The current virtual address to be used
 * 36: (w) CLK_MULT_REG - Sets the clock multiplier
 * 37: (r) INTR_REG - Processes interrupts
 * 38: (r) ERROR_REG - Check for fatal system errors
 * 0 = No errors
 * 1 = Fatal error condition (system should be shutdown)
 * 2 = ??
 * 40: (r/w) INSTR_REG
 * load = Returns the last instruction executed
 * store = Executes an instruction
 * 41: (w) EXC_REG - Cause an exception
 * 
 * loadWord: Translates a virtual address to a physical address
 * 
 * load/store Byte, load/store HalfWord and load/store DoubleWord are not supported
 */
public class Cop0 implements Hardware, Clockable, Bus32bit, Configurable
{
	private static final boolean DEBUG_EXCEPTIONS = false;

	private static final int INDEX_REGISTER = 0;
	private static final int RANDOM_REGISTER = 1;
	private static final int ENTRYLO0_REGISTER = 2;
	private static final int ENTRYLO1_REGISTER = 3;
	private static final int CONTEXT_REGISTER = 4;
	private static final int PAGE_MASK_REGISTER = 5;
	private static final int WIRED_REGISTER = 6;
	private static final int BAD_VADDR_REGISTER = 8;
	private static final int COUNT_REGISTER = 9;
	private static final int ENTRYHI_REGISTER = 10;
	private static final int COMPARE_REGISTER = 11;
	private static final int STATUS_REGISTER = 12;
	private static final int CAUSE_REGISTER = 13;
	private static final int EPC_REGISTER = 14;
	private static final int CONFIG_REGISTER = 16;
	private static final int TAGLO_REGISTER = 28;
	private static final int TAGHI_REGISTER = 29;
	private static final int ERROREPC_REGISTER = 30;
	private static final int FAKE_CAUSE_REGISTER = 32;

	// private static final int CAUSE_EXC_CODE = 0xFF;
	// private static final int CAUSE_IP0 = 0x100;
	// private static final int CAUSE_IP1 = 0x200;
	private static final int CAUSE_IP2 = 0x400;
	// private static final int CAUSE_IP3 = 0x800;
	// private static final int CAUSE_IP4 = 0x1000;
	// private static final int CAUSE_IP5 = 0x2000;
	// private static final int CAUSE_IP6 = 0x4000;
	private static final int CAUSE_IP7 = 0x8000;
	private static final int CAUSE_BD = 0x80000000;

	private static final int STATUS_IE = 0x00000001;
	private static final int STATUS_EXL = 0x00000002;
	private static final int STATUS_ERL = 0x00000004;
	// private static final int STATUS_IP0 = 0x00000100;
	// private static final int STATUS_IP1 = 0x00000200;
	// private static final int STATUS_IP2 = 0x00000400;
	// private static final int STATUS_IP3 = 0x00000800;
	// private static final int STATUS_IP4 = 0x00001000;
	// private static final int STATUS_IP5 = 0x00002000;
	// private static final int STATUS_IP6 = 0x00004000;
	// private static final int STATUS_IP7 = 0x00008000;
	// private static final int STATUS_BEV = 0x00400000;
	// private static final int STATUS_FR = 0x04000000;
	// private static final int STATUS_CU0 = 0x10000000;
	// private static final int STATUS_CU1 = 0x20000000;

	// Exceptions
	private static final int EXC_INT = 0 << 2; /* interrupt */
	// private static final int EXC_MOD = 1<<2; /* Tlb mod */
	private static final int EXC_RMISS = 2 << 2; /* Read Tlb Miss */
	// private static final int EXC_WMISS = 3<<2; /* Write Tlb Miss */
	// private static final int EXC_RADE = 4<<2; /* Read Address Error */
	// private static final int EXC_WADE = 5<<2; /* Write Address Error */
	// private static final int EXC_IBE = 6<<2; /* Instruction Bus Error */
	// private static final int EXC_DBE = 7<<2; /* Data Bus Error */
	// private static final int EXC_SYSCALL = 8<<2; /* SYSCALL */
	// private static final int EXC_BREAK = 9<<2; /* BREAKpoint */
	// private static final int EXC_II = 10<<2; /* Illegal Instruction */
	// private static final int EXC_CPU = 11<<2; /* CoProcessor Unusable */
	// private static final int EXC_OV = 12<<2; /* OVerflow */
	// private static final int EXC_TRAP = 13<<2; /* Trap exception */
	// private static final int EXC_VCEI = 14<<2; /* Virt. Coherency on Inst. fetch */
	// private static final int EXC_FPE = 15<<2; /* Floating Point Exception */
	// private static final int EXC_WATCH = 23<<2; /* Watchpoint reference */
	// private static final int EXC_VCED = 31<<2; /* Virt. Coherency on data read */

	// CPU Registers
	protected static final int CPU_PC_REG = 32;
	protected static final int CPU_LLBIT_REG = 35;
	protected static final int CPU_JMP_DELAY_REG = 37;
	protected static final int CPU_INTERRUPT_REG = 38;

	protected static final int JUMP = 6;

	protected static final int RS = 21;
	protected static final int RT = 16;
	protected static final int RD = 11;
	protected static final int SA = 06;

	private class CPU_ACTION
	{
		public boolean DoSomething;
		public boolean CheckInterrupts;
		public boolean DoInterrupt;
	}

	public static interface OpCode
	{
		public void exec(int inst1, int inst2);
	}

	protected OpCode[] r4300i_CoP0;
	protected OpCode[] r4300i_CoP0_Function;

	protected int[] CP0 = new int[33];
	protected int instruction;

	private int timerCompare;
	private int countPerOp;
	private int wired = 32;
	private int addr;
	private CPU_ACTION cpuAction;
	private boolean useTlb = true;

	protected Bus32bit cpu;
	protected Bus32bit mmu;

	public Cop0()
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

			mmu = (Bus32bit) Class.forName(prop.getProperty("CPU_MMU", "CPU_MMU"), true, loader).newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		((Hardware) mmu).connect(0, this);

		CP0[RANDOM_REGISTER] = 0x1F;
		CP0[COUNT_REGISTER] = 0x5000;
		CP0[CAUSE_REGISTER] = 0x0000005C;
		CP0[CONTEXT_REGISTER] = 0x007FFFF0;
		CP0[EPC_REGISTER] = 0xFFFFFFFF;
		CP0[BAD_VADDR_REGISTER] = 0xFFFFFFFF;
		CP0[ERROREPC_REGISTER] = 0xFFFFFFFF;
		CP0[CONFIG_REGISTER] = 0x0006E463;
		CP0[STATUS_REGISTER] = 0x34000000;

		cpuAction = new CPU_ACTION();
		buildOps();

		changeCompareTimer();
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			cpu = (Bus32bit) bus;
			break;
		}
	}

	@Override
	public void reset()
	{
		timerCompare = 0;
		countPerOp = 0;
		instruction = 0;
		wired = 32;
		addr = 0;
		CP0 = new int[33];
		((Hardware) mmu).reset();
		CP0[RANDOM_REGISTER] = 0x1F;
		CP0[COUNT_REGISTER] = 0x5000;
		CP0[CAUSE_REGISTER] = 0x0000005C;
		CP0[CONTEXT_REGISTER] = 0x007FFFF0;
		CP0[EPC_REGISTER] = 0xFFFFFFFF;
		CP0[BAD_VADDR_REGISTER] = 0xFFFFFFFF;
		CP0[ERROREPC_REGISTER] = 0xFFFFFFFF;
		CP0[CONFIG_REGISTER] = 0x0006E463;
		CP0[STATUS_REGISTER] = 0x34000000;
		cpuAction = new CPU_ACTION();
		changeCompareTimer();
	}

	@Override
	public void clock(long ticks)
	{
		CP0[COUNT_REGISTER] += ticks;
		timerCompare = -1;
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equalsIgnoreCase("MMU")) return mmu;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
	}

	@Override
	public int read32bit(int reg)
	{
		switch (reg)
		{
		case 0:
			return CP0[INDEX_REGISTER];
		case 1:
			update();
			return CP0[RANDOM_REGISTER];
		case 2:
			return CP0[ENTRYLO0_REGISTER];
		case 3:
			return CP0[ENTRYLO1_REGISTER];
		case 4:
			return CP0[CONTEXT_REGISTER];
		case 5:
			return CP0[PAGE_MASK_REGISTER];
		case 6:
			return CP0[WIRED_REGISTER];
		case 7:
			return CP0[7];
		case 8:
			return CP0[BAD_VADDR_REGISTER];
		case 9:
			update();
			return CP0[COUNT_REGISTER];
		case 10:
			return CP0[ENTRYHI_REGISTER];
		case 11:
			return CP0[COMPARE_REGISTER];
		case 12:
			return CP0[STATUS_REGISTER];
		case 13:
			return CP0[CAUSE_REGISTER];
		case 14:
			return CP0[EPC_REGISTER];
		case 15:
			return CP0[15];
		case 16:
			return CP0[CONFIG_REGISTER];
		case 17:
			return CP0[17];
		case 18:
			return CP0[18];
		case 19:
			return CP0[19];
		case 20:
			return CP0[20];
		case 21:
			return CP0[21];
		case 22:
			return CP0[22];
		case 23:
			return CP0[23];
		case 24:
			return CP0[24];
		case 25:
			return CP0[25];
		case 26:
			return CP0[26];
		case 27:
			return CP0[27];
		case 28:
			return CP0[TAGLO_REGISTER];
		case 29:
			return CP0[TAGHI_REGISTER];
		case 30:
			return CP0[ERROREPC_REGISTER];
		case 31:
			return CP0[31];
		case 33:
			return addr;
		case 37:
			update();
			if (timerCompare < 0)
				timerCompareDone();
			if (cpuAction.DoSomething)
			{
				if (cpuAction.CheckInterrupts)
				{
					cpuAction.CheckInterrupts = false;
					if (cpu.read32bit(CPU_INTERRUPT_REG) != 0)
						CP0[FAKE_CAUSE_REGISTER] |= CAUSE_IP2;
					else
						CP0[FAKE_CAUSE_REGISTER] &= ~CAUSE_IP2;
					if ((CP0[STATUS_REGISTER] & STATUS_IE) != 0
							&& (CP0[STATUS_REGISTER] & STATUS_EXL) == 0
							&& (CP0[STATUS_REGISTER] & STATUS_ERL) == 0)
					{
						if ((CP0[STATUS_REGISTER] & CP0[FAKE_CAUSE_REGISTER] & 0xFF00) != 0)
						{
							if (!cpuAction.DoInterrupt)
							{
								cpuAction.DoSomething = true;
								cpuAction.DoInterrupt = true;
							}
						}
					}
				}
				if (cpuAction.DoInterrupt)
				{
					cpuAction.DoInterrupt = false;
					doException(CP0[FAKE_CAUSE_REGISTER] | EXC_INT, addr);
				}

				cpuAction.DoSomething = false;

				if (cpuAction.DoInterrupt)
				{
					cpuAction.DoSomething = true;
				}
			}
			return 0;
		case 38:
			return checkInPermLoop();
		case 40:
			return instruction;
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch (reg)
		{
		case 0:
			CP0[INDEX_REGISTER] = value;
			break;
		case 1:
			break; // Random
		case 2:
			CP0[ENTRYLO0_REGISTER] = value;
			break;
		case 3:
			CP0[ENTRYLO1_REGISTER] = value;
			break;
		case 4:
			CP0[CONTEXT_REGISTER] = value;
			break;
		case 5:
			CP0[PAGE_MASK_REGISTER] = value;
			break;
		case 6:
			CP0[WIRED_REGISTER] = value;
			wired = 32 - value;
			CP0[RANDOM_REGISTER] = 31;
			break;
		case 7:
			break;
		case 8:
			CP0[BAD_VADDR_REGISTER] = value;
			break;
		case 9:
			CP0[COUNT_REGISTER] = value;
			update();
			changeCompareTimer();
			break;
		case 10:
			CP0[ENTRYHI_REGISTER] = value;
			break;
		case 11:
			CP0[COMPARE_REGISTER] = value;
			CP0[FAKE_CAUSE_REGISTER] &= ~CAUSE_IP7;
			update();
			changeCompareTimer();
			break;
		case 12:
			CP0[STATUS_REGISTER] = value;
			break;
		case 13:
			CP0[CAUSE_REGISTER] = value;
			break;
		case 14:
			CP0[EPC_REGISTER] = value;
			break;
		case 15:
			break;
		case 16:
			CP0[CONFIG_REGISTER] = value;
			break;
		case 17:
			break;
		case 18:
			CP0[18] = value;
			break;
		case 19:
			CP0[19] = value;
			break;
		case 20:
			break;
		case 21:
			break;
		case 22:
			break;
		case 23:
			break;
		case 24:
			break;
		case 25:
			break;
		case 26:
			break;
		case 27:
			break;
		case 28:
			CP0[TAGLO_REGISTER] = value;
			break;
		case 29:
			CP0[TAGHI_REGISTER] = value;
			break;
		case 30:
			CP0[ERROREPC_REGISTER] = value;
			break;
		case 31:
			break;
		case 33:
			addr = value;
			break;
		case 36:
			countPerOp = value;
			break;
		case 37:
			checkInterrupts(value);
			break;
		case 40:
			instruction = value;
			r4300i_CoP0[(instruction >> RS) & 0x1F].exec(value, 0);
			break;
		case 41:
			doException(value, addr);
			break;
		case 42:
			CP0[CONTEXT_REGISTER] &= 0xFF80000F;
			CP0[CONTEXT_REGISTER] |= (addr >>> 9) & 0x007FFFF0;
			CP0[ENTRYHI_REGISTER] = addr & 0xFFFFE000;
			tlbReadException(value, addr);
			cpu.write32bit(64, 1);
			break;
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void update()
	{
		int tick = cpu.read32bit(33);
		timerCompare -= tick * countPerOp;
		CP0[COUNT_REGISTER] += tick * countPerOp;
		CP0[RANDOM_REGISTER] -= (tick > wired) ? tick % wired : tick;
		if (CP0[RANDOM_REGISTER] < CP0[WIRED_REGISTER])
			CP0[RANDOM_REGISTER] += wired;
	}

	private void timerCompareDone()
	{
		CP0[FAKE_CAUSE_REGISTER] |= CAUSE_IP7;
		if (cpu.read32bit(CPU_INTERRUPT_REG) != 0)
			CP0[FAKE_CAUSE_REGISTER] |= CAUSE_IP2;
		else
			CP0[FAKE_CAUSE_REGISTER] &= ~CAUSE_IP2;
		if ((CP0[STATUS_REGISTER] & STATUS_IE) != 0
				&& (CP0[STATUS_REGISTER] & STATUS_EXL) == 0
				&& (CP0[STATUS_REGISTER] & STATUS_ERL) == 0)
		{
			if ((CP0[STATUS_REGISTER] & CP0[FAKE_CAUSE_REGISTER] & 0xFF00) != 0)
			{
				if (!cpuAction.DoInterrupt)
				{
					cpuAction.DoSomething = true;
					cpuAction.DoInterrupt = true;
				}
			}
		}
		changeCompareTimer();
	}

	private void changeCompareTimer()
	{
		int nextCompare = CP0[COMPARE_REGISTER] - CP0[COUNT_REGISTER];
		if ((nextCompare & 0x80000000) != 0)
			nextCompare = 0x7FFFFFFF;
		if (nextCompare == 0)
			nextCompare = 0x1;

		int nextTimerCompare = nextCompare - timerCompare;
		if (nextTimerCompare != 0x7FFFFFFF)
			nextTimerCompare += timerCompare;
		timerCompare = 0x7FFFFFFF;
		if (nextTimerCompare < timerCompare)
			timerCompare = nextTimerCompare;

		if (nextTimerCompare == 0x7FFFFFFF)
		{
			nextCompare = CP0[COMPARE_REGISTER] - CP0[COUNT_REGISTER];
			if ((nextCompare & 0x80000000) == 0 && nextCompare != 0x7FFFFFFF) {
				changeCompareTimer();
			}
		}
	}

	private int checkInPermLoop()
	{
		if (cpuAction.DoInterrupt)
			return 0;

		/* Interrupts enabled */
		if ((CP0[STATUS_REGISTER] & STATUS_IE) == 0)
			return 1;
		if ((CP0[STATUS_REGISTER] & STATUS_EXL) != 0)
			return 1;
		if ((CP0[STATUS_REGISTER] & STATUS_ERL) != 0)
			return 1;
		if ((CP0[STATUS_REGISTER] & 0xFF00) == 0)
			return 1;

		update();
		timerCompare -= 5;
		CP0[COUNT_REGISTER] += 5;
		return 2;
	}

	private void doException(int type, int badVaddr)
	{
		if ((CP0[STATUS_REGISTER] & STATUS_EXL) != 0)
		{
			System.err.printf("EXL set in Exception type: %d\n", type);
			return;
		}
		if ((CP0[STATUS_REGISTER] & STATUS_ERL) != 0)
		{
			System.err.printf("ERL set in Exception type: %d\n", type);
			return;
		}
		if ((type & EXC_INT) != 0 && (CP0[STATUS_REGISTER] & STATUS_IE) == 0) { return; }

		CP0[CAUSE_REGISTER] = type;
		CP0[BAD_VADDR_REGISTER] = badVaddr;

		if (cpu.read32bit(CPU_JMP_DELAY_REG) == JUMP)
		{
			CP0[CAUSE_REGISTER] |= CAUSE_BD;
			CP0[EPC_REGISTER] = cpu.read32bit(CPU_PC_REG) - 4;
		}
		else
		{
			CP0[EPC_REGISTER] = cpu.read32bit(CPU_PC_REG);
		}

		CP0[STATUS_REGISTER] |= STATUS_EXL;
		cpu.write32bit(CPU_PC_REG, 0x80000180 - 4);

		if (DEBUG_EXCEPTIONS)
			System.out.printf("-:DoException:%X:%X:%X\n", CP0[STATUS_REGISTER], CP0[CAUSE_REGISTER], CP0[EPC_REGISTER]);
	}

	private void tlbReadException(int type, int badVaddr)
	{
		CP0[CAUSE_REGISTER] = type;
		CP0[BAD_VADDR_REGISTER] = badVaddr;

		if ((CP0[STATUS_REGISTER] & STATUS_EXL) == 0)
		{
			if (cpu.read32bit(CPU_JMP_DELAY_REG) == JUMP)
			{
				CP0[CAUSE_REGISTER] |= CAUSE_BD;
				CP0[EPC_REGISTER] = cpu.read32bit(CPU_PC_REG) - 4;
			}
			else
			{
				CP0[EPC_REGISTER] = cpu.read32bit(CPU_PC_REG);
			}
			CP0[STATUS_REGISTER] |= STATUS_EXL;
			if (((Bus1bit) mmu).read1bit(0))
			{
				cpu.write32bit(CPU_PC_REG, 0x80000180 - 4);
			}
			else
			{
				cpu.write32bit(CPU_PC_REG, 0x80000000 - 4);
			}
		}
		else
		{
			System.err.printf("EXL Set\nAddress (%X) Defined: %s\n", badVaddr, ((Bus1bit) mmu).read1bit(0) ? "TRUE" : "FALSE");
			cpu.write32bit(CPU_PC_REG, 0x80000180 - 4);
		}
	}

	private void checkInterrupts(int cause)
	{
		if (cause != 0)
			CP0[FAKE_CAUSE_REGISTER] |= CAUSE_IP2;
		else
			CP0[FAKE_CAUSE_REGISTER] &= ~CAUSE_IP2;

		if ((CP0[STATUS_REGISTER] & STATUS_IE) != 0
				&& (CP0[STATUS_REGISTER] & STATUS_EXL) == 0
				&& (CP0[STATUS_REGISTER] & STATUS_ERL) == 0)
		{
			if ((CP0[STATUS_REGISTER] & CP0[FAKE_CAUSE_REGISTER] & 0xFF00) != 0)
			{
				if (!cpuAction.DoInterrupt)
				{
					cpuAction.DoSomething = true;
					cpuAction.DoInterrupt = true;
				}
			}
		}
	}

	private void buildOps()
	{
		r4300i_CoP0 = new OpCode[32];
		for (int i = 0; i < 32; i++)
			r4300i_CoP0[i] = R4300i_UnknownOpcode;
		r4300i_CoP0[0] = r4300i_COP0_MF;
		r4300i_CoP0[4] = r4300i_COP0_MT;
		for (int i = 16; i < 32; i++)
			r4300i_CoP0[i] = R4300i_opcode_COP0_CO;

		r4300i_CoP0_Function = new OpCode[64];
		for (int i = 0; i < 64; i++)
			r4300i_CoP0_Function[i] = R4300i_UnknownOpcode;
		r4300i_CoP0_Function[1] = R4300i_opcode_COP0_CO_TLB;
		r4300i_CoP0_Function[2] = R4300i_opcode_COP0_CO_TLB;
		r4300i_CoP0_Function[6] = R4300i_opcode_COP0_CO_TLB;
		r4300i_CoP0_Function[8] = R4300i_opcode_COP0_CO_TLB;
		r4300i_CoP0_Function[24] = r4300i_COP0_CO_ERET;
	}

	/************************* OpCode functions *************************/

	protected OpCode R4300i_opcode_COP0_CO = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			r4300i_CoP0_Function[inst & 0x3F].exec(inst, unused);
		}
	};

	/************************** COP0 functions **************************/

	protected OpCode r4300i_COP0_MF = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			if (((inst >> RD) & 0x1F) == RANDOM_REGISTER || ((inst >> RD) & 0x1F) == COUNT_REGISTER)
				update();
			cpu.write32bit((inst >> RT) & 0x1F, CP0[(inst >> RD) & 0x1F]);
		}
	};

	protected OpCode r4300i_COP0_MT = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			switch ((inst >> RD) & 0x1F)
			{
			case 0: // Index
			case 2: // EntryLo0
			case 3: // EntryLo1
			case 5: // PageMask
			case 10: // Entry Hi
			case 14: // EPC
			case 16: // Config
			case 18: // WatchLo
			case 19: // WatchHi
			case 28: // Tag lo
			case 29: // Tag Hi
				CP0[(inst >> RD) & 0x1F] = cpu.read32bit((inst >> RT) & 0x1F);
				break;
			case 4: // Context
				CP0[CONTEXT_REGISTER] = cpu.read32bit((inst >> RT) & 0x1F) & 0xFF800000;
				break;
			case 6: // Wired
				CP0[WIRED_REGISTER] = cpu.read32bit((inst >> RT) & 0x1F);
				wired = 32 - CP0[WIRED_REGISTER];
				CP0[RANDOM_REGISTER] = 31;
				break;
			case 9: // Count
				CP0[COUNT_REGISTER] = cpu.read32bit((inst >> RT) & 0x1F);
				update();
				changeCompareTimer();
				break;
			case 11: // Compare
				CP0[COMPARE_REGISTER] = cpu.read32bit((inst >> RT) & 0x1F);
				CP0[FAKE_CAUSE_REGISTER] &= ~CAUSE_IP7;
				update();
				changeCompareTimer();
				break;
			case 12: // Status
				CP0[STATUS_REGISTER] = cpu.read32bit((inst >> RT) & 0x1F);
				if ((CP0[STATUS_REGISTER] & 0x18) != 0)
					System.err.printf("Left kernel mode ??\n");
				if (cpu.read32bit(CPU_INTERRUPT_REG) != 0)
					CP0[FAKE_CAUSE_REGISTER] |= CAUSE_IP2;
				else
					CP0[FAKE_CAUSE_REGISTER] &= ~CAUSE_IP2;
				if ((CP0[STATUS_REGISTER] & STATUS_IE) != 0
						&& (CP0[STATUS_REGISTER] & STATUS_EXL) == 0
						&& (CP0[STATUS_REGISTER] & STATUS_ERL) == 0)
				{
					if ((CP0[STATUS_REGISTER] & CP0[FAKE_CAUSE_REGISTER] & 0xFF00) != 0)
					{
						if (!cpuAction.DoInterrupt)
						{
							cpuAction.DoSomething = true;
							cpuAction.DoInterrupt = true;
						}
					}
				}
				break;
			case 13: // cause
				CP0[CAUSE_REGISTER] &= 0xFFFFCFF;
				if ((cpu.read32bit((inst >> RT) & 0x1F) & 0x300) != 0)
					System.err.printf("Set IP0 or IP1\n");
				break;
			default:
				System.err.printf("COP0_MT: Unknown RD: %d\n", (inst >> RD) & 0x1F);
			}
		}
	};

	protected OpCode r4300i_COP0_CO_ERET = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			if ((CP0[STATUS_REGISTER] & STATUS_ERL) != 0)
			{
				cpu.write32bit(CPU_PC_REG, CP0[ERROREPC_REGISTER] - 4);
				CP0[STATUS_REGISTER] &= ~STATUS_ERL;
			}
			else
			{
				cpu.write32bit(CPU_PC_REG, CP0[EPC_REGISTER] - 4);
				CP0[STATUS_REGISTER] &= ~STATUS_EXL;
			}
			cpu.write32bit(CPU_LLBIT_REG, 0);
			if (cpu.read32bit(CPU_INTERRUPT_REG) != 0)
				CP0[FAKE_CAUSE_REGISTER] |= CAUSE_IP2;
			else
				CP0[FAKE_CAUSE_REGISTER] &= ~CAUSE_IP2;
			if ((CP0[STATUS_REGISTER] & STATUS_IE) != 0
					&& (CP0[STATUS_REGISTER] & STATUS_EXL) == 0
					&& (CP0[STATUS_REGISTER] & STATUS_ERL) == 0)
			{
				if ((CP0[STATUS_REGISTER] & CP0[FAKE_CAUSE_REGISTER] & 0xFF00) != 0)
				{
					if (!cpuAction.DoInterrupt)
					{
						cpuAction.DoSomething = true;
						cpuAction.DoInterrupt = true;
					}
				}
			}
		}
	};

	/************************** COP0 CO functions ***********************/

	protected OpCode R4300i_opcode_COP0_CO_TLB = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			if (!useTlb)
				return;
			mmu.write32bit(0, inst);
		}
	};

	/************************** Other functions **************************/

	protected OpCode R4300i_UnknownOpcode = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			System.err.printf("PC:%X ,Unhandled r4300i Cop0 OpCode:%X\n", cpu.read32bit(CPU_PC_REG), inst);
			System.exit(0);
		}
	};
}
