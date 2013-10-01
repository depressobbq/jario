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

import jario.hardware.Bus16bit;
import jario.hardware.Bus32bit;
import jario.hardware.Bus64bit;
import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

/**
 * Registers:
 * 0 - 31: (r/w) General Purpose CPU Registers (32 bit)
 * Accessed with load/store Byte, load/store HalfWord,
 * load/store Word and load/store DoubleWord
 * 32: (r/w) PC_REG - The address of the current instruction (Program Counter)
 * 33: (r/w) CLOCK_REG - Manipulates the system clock
 * read - returns the number of clock ticks since the last read
 * write - sets the clock multiplier
 * 34: (w) CPU_START_REG - starts the cpu running
 * 35: (r/w) LLBIT_REG - Load/Link Bit
 * 36: (r/w) JMP_ADDR_REG - The instruction address that the pc will jump to
 * 37: (r/w) JMP_DELAY_REG - Information about how the JMP_ADDR_REG will be used
 * 0 = Normal, 3 = Delay Slot, 6 = Jump
 * 38: (r) INTR_REG - Returns the MIPS Interface interrupt status
 * 39: (r) JMP_TEST_REG - Test the safety of jumping to the address in JMP_ADDR_REG
 * if the jump is unsafe (causing a loop) then the system will be shut down
 * 40: (r) INSTR_REG
 * load = Returns the last instruction executed
 * 41/42: (r/w) MULT_HI_REG - Stores the result of integer mult/div operations
 * reg 41 - upper 32 bits
 * reg 42 - lower 32 bits
 * 43/44: (r/w) MULT_LO_REG - Stores the result of integer mult/div operations
 * reg 43 - upper 32 bits
 * reg 44 - lower 32 bits
 */
public class Cpu implements Hardware, Clockable, Bus8bit, Bus16bit, Bus32bit, Bus64bit, Configurable
{
	// R4300i OpCodes
	private static final int R4300i_SPECIAL = 0;
	private static final int R4300i_REGIMM = 1;
	private static final int R4300i_J = 2;
	private static final int R4300i_JAL = 3;
	private static final int R4300i_BEQ = 4;
	private static final int R4300i_BNE = 5;
	private static final int R4300i_BLEZ = 6;
	private static final int R4300i_BGTZ = 7;
	private static final int R4300i_ADDI = 8;
	private static final int R4300i_ADDIU = 9;
	private static final int R4300i_SLTI = 10;
	private static final int R4300i_SLTIU = 11;
	private static final int R4300i_ANDI = 12;
	private static final int R4300i_ORI = 13;
	private static final int R4300i_XORI = 14;
	private static final int R4300i_LUI = 15;
	private static final int R4300i_CP0 = 16;
	private static final int R4300i_CP1 = 17;
	private static final int R4300i_BEQL = 20;
	private static final int R4300i_BNEL = 21;
	private static final int R4300i_BLEZL = 22;
	private static final int R4300i_BGTZL = 23;
	private static final int R4300i_DADDI = 24;
	private static final int R4300i_DADDIU = 25;
	private static final int R4300i_LDL = 26;
	private static final int R4300i_LDR = 27;
	private static final int R4300i_LB = 32;
	private static final int R4300i_LH = 33;
	private static final int R4300i_LWL = 34;
	private static final int R4300i_LW = 35;
	private static final int R4300i_LBU = 36;
	private static final int R4300i_LHU = 37;
	private static final int R4300i_LWR = 38;
	private static final int R4300i_LWU = 39;
	private static final int R4300i_SB = 40;
	private static final int R4300i_SH = 41;
	private static final int R4300i_SWL = 42;
	private static final int R4300i_SW = 43;
	private static final int R4300i_SDL = 44;
	private static final int R4300i_SDR = 45;
	private static final int R4300i_SWR = 46;
	private static final int R4300i_CACHE = 47;
	private static final int R4300i_LL = 48;
	private static final int R4300i_LWC1 = 49;
	private static final int R4300i_LWC2 = 0x32;
	private static final int R4300i_LLD = 0x34;
	private static final int R4300i_LDC1 = 53;
	private static final int R4300i_LDC2 = 0x36;
	private static final int R4300i_LD = 55;
	private static final int R4300i_SC = 0x38;
	private static final int R4300i_SWC1 = 57;
	private static final int R4300i_SWC2 = 0x3A;
	private static final int R4300i_SCD = 0x3C;
	private static final int R4300i_SDC1 = 61;
	private static final int R4300i_SDC2 = 62;
	private static final int R4300i_SD = 63;

	/* R4300i Special opcodes */
	private static final int R4300i_SPECIAL_SLL = 0;
	private static final int R4300i_SPECIAL_SRL = 2;
	private static final int R4300i_SPECIAL_SRA = 3;
	private static final int R4300i_SPECIAL_SLLV = 4;
	private static final int R4300i_SPECIAL_SRLV = 6;
	private static final int R4300i_SPECIAL_SRAV = 7;
	private static final int R4300i_SPECIAL_JR = 8;
	private static final int R4300i_SPECIAL_JALR = 9;
	private static final int R4300i_SPECIAL_SYSCALL = 12;
	private static final int R4300i_SPECIAL_BREAK = 13;
	private static final int R4300i_SPECIAL_SYNC = 15;
	private static final int R4300i_SPECIAL_MFHI = 16;
	private static final int R4300i_SPECIAL_MTHI = 17;
	private static final int R4300i_SPECIAL_MFLO = 18;
	private static final int R4300i_SPECIAL_MTLO = 19;
	private static final int R4300i_SPECIAL_DSLLV = 20;
	private static final int R4300i_SPECIAL_DSRLV = 22;
	private static final int R4300i_SPECIAL_DSRAV = 23;
	private static final int R4300i_SPECIAL_MULT = 24;
	private static final int R4300i_SPECIAL_MULTU = 25;
	private static final int R4300i_SPECIAL_DIV = 26;
	private static final int R4300i_SPECIAL_DIVU = 27;
	private static final int R4300i_SPECIAL_DMULT = 28;
	private static final int R4300i_SPECIAL_DMULTU = 29;
	private static final int R4300i_SPECIAL_DDIV = 30;
	private static final int R4300i_SPECIAL_DDIVU = 31;
	private static final int R4300i_SPECIAL_ADD = 32;
	private static final int R4300i_SPECIAL_ADDU = 33;
	private static final int R4300i_SPECIAL_SUB = 34;
	private static final int R4300i_SPECIAL_SUBU = 35;
	private static final int R4300i_SPECIAL_AND = 36;
	private static final int R4300i_SPECIAL_OR = 37;
	private static final int R4300i_SPECIAL_XOR = 38;
	private static final int R4300i_SPECIAL_NOR = 39;
	private static final int R4300i_SPECIAL_SLT = 42;
	private static final int R4300i_SPECIAL_SLTU = 43;
	private static final int R4300i_SPECIAL_DADD = 44;
	private static final int R4300i_SPECIAL_DADDU = 45;
	private static final int R4300i_SPECIAL_DSUB = 46;
	private static final int R4300i_SPECIAL_DSUBU = 47;
	private static final int R4300i_SPECIAL_TGE = 48;
	private static final int R4300i_SPECIAL_TGEU = 49;
	private static final int R4300i_SPECIAL_TLT = 50;
	private static final int R4300i_SPECIAL_TLTU = 51;
	private static final int R4300i_SPECIAL_TEQ = 52;
	private static final int R4300i_SPECIAL_TNE = 54;
	private static final int R4300i_SPECIAL_DSLL = 56;
	private static final int R4300i_SPECIAL_DSRL = 58;
	private static final int R4300i_SPECIAL_DSRA = 59;
	private static final int R4300i_SPECIAL_DSLL32 = 60;
	private static final int R4300i_SPECIAL_DSRL32 = 62;
	private static final int R4300i_SPECIAL_DSRA32 = 63;

	/* R4300i RegImm opcodes */
	private static final int R4300i_REGIMM_BLTZ = 0;
	private static final int R4300i_REGIMM_BGEZ = 1;
	private static final int R4300i_REGIMM_BLTZL = 2;
	private static final int R4300i_REGIMM_BGEZL = 3;
	private static final int R4300i_REGIMM_TGEI = 0x08;
	private static final int R4300i_REGIMM_TGEIU = 0x09;
	private static final int R4300i_REGIMM_TLTI = 0x0A;
	private static final int R4300i_REGIMM_TLTIU = 0x0B;
	private static final int R4300i_REGIMM_TEQI = 0x0C;
	private static final int R4300i_REGIMM_TNEI = 0x0E;
	private static final int R4300i_REGIMM_BLTZAL = 0x10;
	private static final int R4300i_REGIMM_BGEZAL = 17;
	private static final int R4300i_REGIMM_BLTZALL = 0x12;
	private static final int R4300i_REGIMM_BGEZALL = 0x13;

	/* R4300i COP0 opcodes */
	private static final int R4300i_COP0_MF = 0;
	private static final int R4300i_COP0_MT = 4;

	/* R4300i COP0 CO opcodes */
	private static final int R4300i_COP0_CO_TLBR = 1;
	private static final int R4300i_COP0_CO_TLBWI = 2;
	private static final int R4300i_COP0_CO_TLBWR = 6;
	private static final int R4300i_COP0_CO_TLBP = 8;
	private static final int R4300i_COP0_CO_ERET = 24;

	/* R4300i COP1 opcodes */
	private static final int R4300i_COP1_MF = 0;
	private static final int R4300i_COP1_DMF = 1;
	private static final int R4300i_COP1_CF = 2;
	private static final int R4300i_COP1_MT = 4;
	private static final int R4300i_COP1_DMT = 5;
	private static final int R4300i_COP1_CT = 6;
	private static final int R4300i_COP1_BC = 8;
	private static final int R4300i_COP1_S = 16;
	private static final int R4300i_COP1_D = 17;
	private static final int R4300i_COP1_W = 20;
	private static final int R4300i_COP1_L = 21;

	/* R4300i COP1 BC opcodes */
	private static final int R4300i_COP1_BC_BCF = 0;
	private static final int R4300i_COP1_BC_BCT = 1;
	private static final int R4300i_COP1_BC_BCFL = 2;
	private static final int R4300i_COP1_BC_BCTL = 3;

	private static final int R4300i_COP1_FUNCT_ADD = 0;
	private static final int R4300i_COP1_FUNCT_SUB = 1;
	private static final int R4300i_COP1_FUNCT_MUL = 2;
	private static final int R4300i_COP1_FUNCT_DIV = 3;
	private static final int R4300i_COP1_FUNCT_SQRT = 4;
	private static final int R4300i_COP1_FUNCT_ABS = 5;
	private static final int R4300i_COP1_FUNCT_MOV = 6;
	private static final int R4300i_COP1_FUNCT_NEG = 7;
	private static final int R4300i_COP1_FUNCT_ROUND_L = 8;
	private static final int R4300i_COP1_FUNCT_TRUNC_L = 9;
	private static final int R4300i_COP1_FUNCT_CEIL_L = 10;
	private static final int R4300i_COP1_FUNCT_FLOOR_L = 11;
	private static final int R4300i_COP1_FUNCT_ROUND_W = 12;
	private static final int R4300i_COP1_FUNCT_TRUNC_W = 13;
	private static final int R4300i_COP1_FUNCT_CEIL_W = 14;
	private static final int R4300i_COP1_FUNCT_FLOOR_W = 15;
	private static final int R4300i_COP1_FUNCT_CVT_S = 32;
	private static final int R4300i_COP1_FUNCT_CVT_D = 33;
	private static final int R4300i_COP1_FUNCT_CVT_W = 36;
	private static final int R4300i_COP1_FUNCT_CVT_L = 37;
	private static final int R4300i_COP1_FUNCT_C_F = 48;
	private static final int R4300i_COP1_FUNCT_C_UN = 49;
	private static final int R4300i_COP1_FUNCT_C_EQ = 50;
	private static final int R4300i_COP1_FUNCT_C_UEQ = 51;
	private static final int R4300i_COP1_FUNCT_C_OLT = 52;
	private static final int R4300i_COP1_FUNCT_C_ULT = 53;
	private static final int R4300i_COP1_FUNCT_C_OLE = 54;
	private static final int R4300i_COP1_FUNCT_C_ULE = 55;
	private static final int R4300i_COP1_FUNCT_C_SF = 56;
	private static final int R4300i_COP1_FUNCT_C_NGLE = 57;
	private static final int R4300i_COP1_FUNCT_C_SEQ = 58;
	private static final int R4300i_COP1_FUNCT_C_NGL = 59;
	private static final int R4300i_COP1_FUNCT_C_LT = 60;
	private static final int R4300i_COP1_FUNCT_C_NGE = 61;
	private static final int R4300i_COP1_FUNCT_C_LE = 62;
	private static final int R4300i_COP1_FUNCT_C_NGT = 63;

	// CP0
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

	// extra cop0 registers
	private static final int COP0_ADDRESS_REG = 33;
	private static final int COP0_INTR_REG = 37;
	private static final int COP0_ERROR_REG = 38;
	private static final int COP0_MEM_RW_REG = 39;
	private static final int COP0_INSTR_REG = 40;
	private static final int COP0_EXC_REG = 41;

	private static final int CAUSE_EXC_CODE = 0xFF;
	private static final int CAUSE_IP0 = 0x100;
	private static final int CAUSE_IP1 = 0x200;
	private static final int CAUSE_IP2 = 0x400;
	private static final int CAUSE_IP3 = 0x800;
	private static final int CAUSE_IP4 = 0x1000;
	private static final int CAUSE_IP5 = 0x2000;
	private static final int CAUSE_IP6 = 0x4000;
	private static final int CAUSE_IP7 = 0x8000;
	private static final int CAUSE_BD = 0x80000000;

	private static final int STATUS_IE = 0x00000001;
	private static final int STATUS_EXL = 0x00000002;
	private static final int STATUS_ERL = 0x00000004;
	private static final int STATUS_IP0 = 0x00000100;
	private static final int STATUS_IP1 = 0x00000200;
	private static final int STATUS_IP2 = 0x00000400;
	private static final int STATUS_IP3 = 0x00000800;
	private static final int STATUS_IP4 = 0x00001000;
	private static final int STATUS_IP5 = 0x00002000;
	private static final int STATUS_IP6 = 0x00004000;
	private static final int STATUS_IP7 = 0x00008000;
	private static final int STATUS_BEV = 0x00400000;
	private static final int STATUS_FR = 0x04000000;
	private static final int STATUS_CU0 = 0x10000000;
	private static final int STATUS_CU1 = 0x20000000;

	private static final int EXC_INT = 0 << 2; /* interrupt */
	private static final int EXC_MOD = 1 << 2; /* Tlb mod */
	private static final int EXC_RMISS = 2 << 2; /* Read Tlb Miss */
	private static final int EXC_WMISS = 3 << 2; /* Write Tlb Miss */
	private static final int EXC_RADE = 4 << 2; /* Read Address Error */
	private static final int EXC_WADE = 5 << 2; /* Write Address Error */
	private static final int EXC_IBE = 6 << 2; /* Instruction Bus Error */
	private static final int EXC_DBE = 7 << 2; /* Data Bus Error */
	private static final int EXC_SYSCALL = 8 << 2; /* SYSCALL */
	private static final int EXC_BREAK = 9 << 2; /* BREAKpoint */
	private static final int EXC_II = 10 << 2; /* Illegal Instruction */
	private static final int EXC_CPU = 11 << 2; /* CoProcessor Unusable */
	private static final int EXC_OV = 12 << 2; /* OVerflow */
	private static final int EXC_TRAP = 13 << 2; /* Trap exception */
	private static final int EXC_VCEI = 14 << 2; /* Virt. Coherency on Inst. fetch */
	private static final int EXC_FPE = 15 << 2; /* Floating Point Exception */
	private static final int EXC_WATCH = 23 << 2; /* Watchpoint reference */
	private static final int EXC_VCED = 31 << 2; /* Virt. Coherency on data read */

	private static final int CE_COP0 = 0 << 28;
	private static final int CE_COP1 = 1 << 28;

	// FPCR
	private static final int REVISION_REGISTER = 0;
	private static final int FSTATUS_REGISTER = 31;

	// extra cop1 (fp) registers
	private static final int COP1_FP_EXT_REG = 64;
	private static final int COP1_INSTR_REG = 65;

	private static final int FPCSR_FS = 0x01000000; /* flush denorm to zero */
	private static final int FPCSR_C = 0x00800000; /* condition bit */
	private static final int FPCSR_CE = 0x00020000; /* cause: unimplemented operation */
	private static final int FPCSR_CV = 0x00010000; /* cause: invalid operation */
	private static final int FPCSR_CZ = 0x00008000; /* cause: division by zero */
	private static final int FPCSR_CO = 0x00004000; /* cause: overflow */
	private static final int FPCSR_CU = 0x00002000; /* cause: underflow */
	private static final int FPCSR_CI = 0x00001000; /* cause: inexact operation */
	private static final int FPCSR_EV = 0x00000800; /* enable: invalid operation */
	private static final int FPCSR_EZ = 0x00000400; /* enable: division by zero */
	private static final int FPCSR_EO = 0x00000200; /* enable: overflow */
	private static final int FPCSR_EU = 0x00000100; /* enable: underflow */
	private static final int FPCSR_EI = 0x00000080; /* enable: inexact operation */
	private static final int FPCSR_FV = 0x00000040; /* flag: invalid operation */
	private static final int FPCSR_FZ = 0x00000020; /* flag: division by zero */
	private static final int FPCSR_FO = 0x00000010; /* flag: overflow */
	private static final int FPCSR_FU = 0x00000008; /* flag: underflow */
	private static final int FPCSR_FI = 0x00000004; /* flag: inexact operation */
	private static final int FPCSR_RM_MASK = 0x00000003; /* rounding mode mask */
	private static final int FPCSR_RM_RN = 0x00000000; /* round to nearest */
	private static final int FPCSR_RM_RZ = 0x00000001; /* round to zero */
	private static final int FPCSR_RM_RP = 0x00000002; /* round to positive infinity */
	private static final int FPCSR_RM_RM = 0x00000003; /* round to negative infinity */

	private static final int NORMAL = 0;
	private static final int DO_DELAY_SLOT = 1;
	private static final int DO_END_DELAY_SLOT = 2;
	private static final int DELAY_SLOT = 3;
	private static final int END_DELAY_SLOT = 4;
	private static final int LIKELY_DELAY_SLOT = 5;
	private static final int JUMP = 6;
	private static final int DELAY_SLOT_DONE = 7;
	private static final int LIKELY_DELAY_SLOT_DONE = 8;
	private static final int END_BLOCK = 9;

	private static final int OP = 26;
	private static final int RS = 21;
	private static final int RT = 16;
	private static final int RD = 11;
	private static final int SA = 06;

	public static interface OpCode
	{
		public void exec(int inst1, int inst2);
	}

	private class CachedOpcode
	{
		public int inst;
		public boolean cached;
		public OpCode code;
	}

	private static long[] LDL_MASK =
	{
			0x00000000000000L,
			0x000000000000FFL,
			0x0000000000FFFFL,
			0x00000000FFFFFFL,
			0x000000FFFFFFFFL,
			0x0000FFFFFFFFFFL,
			0x00FFFFFFFFFFFFL,
			0xFFFFFFFFFFFFFFL
	};

	private static int[] LDL_SHIFT = { 0, 8, 16, 24, 32, 40, 48, 56 };

	private static long[] LDR_MASK =
	{
			0xFFFFFFFFFFFFFF00L,
			0xFFFFFFFFFFFF0000L,
			0xFFFFFFFFFF000000L,
			0xFFFFFFFF00000000L,
			0xFFFFFF0000000000L,
			0xFFFF000000000000L,
			0xFF00000000000000L,
			0x0000000000000000L
	};

	private static int[] LDR_SHIFT = { 56, 48, 40, 32, 24, 16, 8, 0 };

	private static int[] LWL_MASK =
	{
			0x000000,
			0x0000FF,
			0x00FFFF,
			0xFFFFFF
	};

	private static int[] LWL_SHIFT = { 0, 8, 16, 24 };

	private static int[] LWR_MASK =
	{
			0xFFFFFF00,
			0xFFFF0000,
			0xFF000000,
			0x00000000
	};

	private static int[] LWR_SHIFT = { 24, 16, 8, 0 };

	private static int[] SWL_MASK =
	{
			0x00000000,
			0xFF000000,
			0xFFFF0000,
			0xFFFFFF00
	};

	private static int[] SWL_SHIFT = { 0, 8, 16, 24 };

	private static long[] SDL_MASK =
	{
			0x0000000000000000L,
			0xFF00000000000000L,
			0xFFFF000000000000L,
			0xFFFFFF0000000000L,
			0xFFFFFFFF00000000L,
			0xFFFFFFFFFF000000L,
			0xFFFFFFFFFFFF0000L,
			0xFFFFFFFFFFFFFF00L
	};

	private static int[] SDL_SHIFT = { 0, 8, 16, 24, 32, 40, 48, 56 };

	private static long[] SDR_MASK =
	{
			0x00FFFFFFFFFFFFFFL,
			0x0000FFFFFFFFFFFFL,
			0x000000FFFFFFFFFFL,
			0x00000000FFFFFFFFL,
			0x0000000000FFFFFFL,
			0x000000000000FFFFL,
			0x00000000000000FFL,
			0x0000000000000000L
	};

	private static int[] SDR_SHIFT = { 56, 48, 40, 32, 24, 16, 8, 0 };

	private static int[] SWR_MASK =
	{
			0x00FFFFFF,
			0x0000FFFF,
			0x000000FF,
			0x00000000
	};

	private static int[] SWR_SHIFT = { 24, 16, 8, 0 };

	protected int nextInstruction;
	protected int pc;
	protected long[] GPR = new long[32];
	protected int jumpToLocation;
	protected long HI;
	protected long LO;
	protected int tmpWord;
	protected long tmpDouble;
	protected int instruction;
	protected int target;
	protected int rs;
	protected int base;
	protected int rt;
	protected short offset;
	protected int rd;
	protected int sa;
	protected int funct;
	protected int currentInstr;

	private int llBit;
	private int llAddr;
	private CachedOpcode[] cachedOpcodes;
	private CachedOpcode cachedOp;
	private OpCode cachedCode;
	private OpCode[] r4300i_Opcode;
	private OpCode[] r4300i_Special;
	private OpCode[] r4300i_Regimm;
	private long[] tmpDmultu = new long[3];
	private int tmpOp;
	private int tmpRs;
	private int tmpRt;
	private int tmpRd;
	private int tmpFunct;
	private boolean cacheInstructions;
	private int tick;
	private int tickTimer;
	private int countPerOp;
	private int mode32;
	private boolean running;

	private boolean tableException;

	private Bus8bit mi8bit;
	private Bus16bit mi16bit;
	private Bus32bit mi32bit;
	private Bus64bit mi64bit;
	protected Bus32bit cop0;
	protected Bus32bit mmu;
	protected Bus32bit cop1;
	protected Bus32bit timer;

	public Cpu()
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

			cop0 = (Bus32bit) Class.forName(prop.getProperty("CPU_COP0", "CPU_COP0"), true, loader).newInstance();
			cop1 = (Bus32bit) Class.forName(prop.getProperty("CPU_COP1", "CPU_COP1"), true, loader).newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		((Hardware) cop0).connect(0, this);
		((Hardware) cop1).connect(0, this);
		mmu = (Bus32bit) ((Configurable) cop0).readConfig("MMU");

		LO = 0x0;
		HI = 0x0;
		cachedOpcodes = new CachedOpcode[0x402000 >>> 2]; // 1050624b (1Mb)
		for (int i = 0; i < (0x402000 >>> 2); i++)
		{
			cachedOpcodes[i] = new CachedOpcode();
		}
		buildOps();
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 4:
			mi8bit = (Bus8bit) bus;
			mi16bit = (Bus16bit) bus;
			mi32bit = (Bus32bit) bus;
			mi64bit = (Bus64bit) bus;
			break;
		case 5:
			timer = (Bus32bit) bus;
			break;
		}
	}

	@Override
	public void reset()
	{
		running = false;
		nextInstruction = NORMAL;
		pc = 0;
		GPR = new long[32];
		jumpToLocation = 0;
		HI = 0L;
		LO = 0L;
		tmpWord = 0;
		tmpDouble = 0L;
		instruction = 0;
		target = 0;
		rs = 0;
		base = 0;
		rt = 0;
		offset = 0;
		rd = 0;
		sa = 0;
		funct = 0;
		currentInstr = 0;
		llBit = 0;
		llAddr = 0;
		cachedOp = null;
		cachedCode = null;
		tmpDmultu = new long[3];
		tmpOp = 0;
		tmpRs = 0;
		tmpRt = 0;
		tmpRd = 0;
		tmpFunct = 0;
		tick = 0;
		tickTimer = 0;
		countPerOp = 0;
		mode32 = 0;
		for (int i = 0; i < cachedOpcodes.length; i++)
		{
			cachedOpcodes[i].cached = false;
		}

		((Hardware) cop0).reset();
		((Hardware) cop1).reset();
	}

	@Override
	public void clock(long clocks)
	{
		if (clocks != 0L)
			startEmulation();
		else
			running = false;
	}

	private int readRegister(int reg)
	{
		switch (reg)
		{
		case 32:
			return pc;
		case 33:
			int tmp = tick;
			tick = 0;
			return tmp;
		case 35:
			return llBit;
		case 37:
			return nextInstruction;
		case 38:
			return mi32bit.read32bit(0x0430000C) & mi32bit.read32bit(0x04300008);
		case 39:
			return testInterpreterJump();
		case 40:
			return instruction;
		case 41:
			return (int) (HI >> 32);
		case 42:
			return (int) HI;
		case 43:
			return (int) (LO >> 32);
		case 44:
			return (int) LO;
		default:
			return 0;
		}
	}

	private void writeRegister(int reg, int value)
	{
		switch (reg)
		{
		case 32:
			if (!running)
				pc = value;
			else
			{
				jumpToLocation = value + 4;
				nextInstruction = JUMP;
			}
			break;
		case 33:
			countPerOp = value;
			cop0.write32bit(36, value);
			break;
		case 35:
			llBit = value;
			break;
		case 38:
			cop0.write32bit(37, value);
			break;
		case 40:
			if (value == 0)
			{
				// nullify delay slot
				jumpToLocation = pc + 8;
				nextInstruction = JUMP;
			}
			else
			{
				nextInstruction = DELAY_SLOT;
			}
			break;
		case 41:
			HI = ((value & 0xFFFFFFFFL) << 32) | (HI & 0xFFFFFFFFL);
			break;
		case 42:
			HI = (HI & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
			break;
		case 43:
			LO = ((value & 0xFFFFFFFFL) << 32) | (LO & 0xFFFFFFFFL);
			break;
		case 44:
			LO = (LO & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
			break;
		case 64:
			tableException = true;
			break;
		}
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("instructioncache")) return cacheInstructions;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("instructioncache")) cacheInstructions = (Boolean) value;
	}

	@Override
	public byte read8bit(int pAddr)
	{
		return (byte) GPR[pAddr];
	}

	@Override
	public short read16bit(int pAddr)
	{
		return (short) GPR[pAddr];
	}

	@Override
	public int read32bit(int pAddr)
	{
		if (pAddr < 32)
			return (int) GPR[pAddr];
		else
			return readRegister(pAddr);
	}

	@Override
	public long read64bit(int pAddr)
	{
		return GPR[pAddr];
	}

	@Override
	public void write8bit(int pAddr, byte value)
	{
		GPR[pAddr] = value;
	}

	@Override
	public void write16bit(int pAddr, short value)
	{
		GPR[pAddr] = value;
	}

	@Override
	public void write32bit(int pAddr, int value)
	{
		if (pAddr < 32)
			GPR[pAddr] = value;
		else
			writeRegister(pAddr, value);
	}

	@Override
	public void write64bit(int pAddr, long value)
	{
		GPR[pAddr] = value;
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void startEmulation()
	{
		running = true;
		startInterpreterCPU();
	}

	// The main emulation loop
	private void startInterpreterCPU()
	{
		while (running)
		{
			int pAddr = mmu.read32bit(pc);
			if (tableException)
			{
				tableException = false;
				pc = jumpToLocation;
				nextInstruction = NORMAL;
				continue;
			}
			if (cacheInstructions)
			{
				cachedOp = cachedOpcodes[(pAddr < 0x400000) ? (pAddr >>> 2) : ((pAddr - 0x3C00000) >>> 2)];
				if (cachedOp.cached)
				{
					instruction = cachedOp.inst;
					cachedCode = cachedOp.code;
				}
				else
				{
					cachedOp.cached = true;
					cachedOp.inst = instruction = mi32bit.read32bit(pAddr);
					cachedCode = cachedOp.code = (((instruction >> OP) & 0x3F) == 0) ? r4300i_Special[instruction & 0x3F] : r4300i_Opcode[(instruction >> OP) & 0x3F];
				}
			}
			else
			{
				instruction = mi32bit.read32bit(pAddr);
			}

			tick++;
			tickTimer++;

			if (cacheInstructions)
				cachedCode.exec(instruction, 0);
			else
				r4300i_Opcode[((instruction >> OP) & 0x3F)].exec(instruction, 0);

			switch (nextInstruction)
			{
			case NORMAL:
				pc += 4;
				continue;
			case DELAY_SLOT:
				nextInstruction = JUMP;
				pc += 4;
				continue;
			case JUMP:
				pc = jumpToLocation;
				nextInstruction = NORMAL;
				((Clockable) timer).clock(tickTimer * countPerOp);
				tickTimer = 0;
				cop0.read32bit(COP0_INTR_REG);
				pc = jumpToLocation;
				nextInstruction = NORMAL;
				continue;
			default:
				System.err.printf("%X:Invalid next-instruction type:%d\n", pc, nextInstruction);
			}
		}
	}

	private int testInterpreterJump()
	{
		if (delaySlotEffectsCompare() == 0)
		{
			int error = cop0.read32bit(COP0_ERROR_REG);
			if (error == 1)
			{
				System.err.printf("In a permanent loop that can not be exited\n\nEmulation will now stop\n");
				System.exit(0);
			}
			else if (error == 2)
			{
				/* check sound playing */
				/* check RSP running */
				/* check RDP running */

				((Clockable) timer).clock(tickTimer * countPerOp + 5);
				tickTimer = 0;

				int time = timer.read32bit(4);
				if (time > 0)
				{
					((Clockable) cop0).clock(time + 1);
					timer.write32bit(4, -1);
				}
			}
		}
		return 1;
	}

	private int delaySlotEffectsCompare()
	{
		if (pc != jumpToLocation)
			return 1;

		int tmpInstr;
		int pAddr = mmu.read32bit(pc + 4);
		if (tableException)
		{
			tableException = false;
			return 1;
		}
		if (cacheInstructions)
		{
			cachedOp = cachedOpcodes[(pAddr < 0x400000) ? (pAddr >>> 2) : ((pAddr - 0x3C00000) >>> 2)];
			if (cachedOp.cached)
			{
				tmpInstr = cachedOp.inst;
			}
			else
			{
				cachedOp.cached = true;
				tmpInstr = cachedOp.inst = mi32bit.read32bit(pAddr);
				cachedOp.code = (((tmpInstr >> 26) & 0x3F) == 0) ? r4300i_Special[tmpInstr & 0x3F] : r4300i_Opcode[(tmpInstr >> 26) & 0x3F];
			}
		}
		else
		{
			tmpInstr = mi32bit.read32bit(pAddr);
		}
		tmpOp = (tmpInstr >> 26) & 0x3F;
		tmpRs = (tmpInstr >> 21) & 0x1F;
		tmpRt = (tmpInstr >> 16) & 0x1F;
		tmpRd = (tmpInstr >> 11) & 0x1F;
		tmpFunct = (tmpInstr) & 0x3F;

		int reg1 = (instruction >> RS) & 0x1F;
		int reg2 = (instruction >> RT) & 0x1F;

		switch (tmpOp)
		{
		case R4300i_SPECIAL:
			switch (tmpFunct)
			{
			case R4300i_SPECIAL_SLL:
			case R4300i_SPECIAL_SRL:
			case R4300i_SPECIAL_SRA:
			case R4300i_SPECIAL_SLLV:
			case R4300i_SPECIAL_SRLV:
			case R4300i_SPECIAL_SRAV:
			case R4300i_SPECIAL_MFHI:
			case R4300i_SPECIAL_MTHI:
			case R4300i_SPECIAL_MFLO:
			case R4300i_SPECIAL_MTLO:
			case R4300i_SPECIAL_DSLLV:
			case R4300i_SPECIAL_DSRLV:
			case R4300i_SPECIAL_DSRAV:
			case R4300i_SPECIAL_ADD:
			case R4300i_SPECIAL_ADDU:
			case R4300i_SPECIAL_SUB:
			case R4300i_SPECIAL_SUBU:
			case R4300i_SPECIAL_AND:
			case R4300i_SPECIAL_OR:
			case R4300i_SPECIAL_XOR:
			case R4300i_SPECIAL_NOR:
			case R4300i_SPECIAL_SLT:
			case R4300i_SPECIAL_SLTU:
			case R4300i_SPECIAL_DADD:
			case R4300i_SPECIAL_DADDU:
			case R4300i_SPECIAL_DSUB:
			case R4300i_SPECIAL_DSUBU:
			case R4300i_SPECIAL_DSLL:
			case R4300i_SPECIAL_DSRL:
			case R4300i_SPECIAL_DSRA:
			case R4300i_SPECIAL_DSLL32:
			case R4300i_SPECIAL_DSRL32:
			case R4300i_SPECIAL_DSRA32:
				if (tmpRd == 0) { return 0; }
				if (tmpRd == reg1) { return 1; }
				if (tmpRd == reg2) { return 1; }
				break;
			case R4300i_SPECIAL_MULT:
			case R4300i_SPECIAL_MULTU:
			case R4300i_SPECIAL_DIV:
			case R4300i_SPECIAL_DIVU:
			case R4300i_SPECIAL_DMULT:
			case R4300i_SPECIAL_DMULTU:
			case R4300i_SPECIAL_DDIV:
			case R4300i_SPECIAL_DDIVU:
				break;
			default:
				return 1;
			}
			break;
		case R4300i_CP0:
			switch (tmpRs)
			{
			case R4300i_COP0_MT:
				break;
			case R4300i_COP0_MF:
				if (tmpRt == 0) { return 0; }
				if (tmpRt == reg1) { return 1; }
				if (tmpRt == reg2) { return 1; }
				break;
			default:
				if ((tmpRs & 0x10) != 0)
				{
					switch ((instruction & 0x3F))
					{
					case R4300i_COP0_CO_TLBR:
						break;
					case R4300i_COP0_CO_TLBWI:
						break;
					case R4300i_COP0_CO_TLBWR:
						break;
					case R4300i_COP0_CO_TLBP:
						break;
					default:
						return 1;
					}
				}
				else
				{
					return 1;
				}
			}
			break;
		case R4300i_CP1:
			switch (tmpRs)
			{
			case R4300i_COP1_MF:
				if (tmpRt == 0) { return 0; }
				if (tmpRt == reg1) { return 1; }
				if (tmpRt == reg2) { return 1; }
				break;
			case R4300i_COP1_CF:
				break;
			case R4300i_COP1_MT:
				break;
			case R4300i_COP1_CT:
				break;
			case R4300i_COP1_S:
				break;
			case R4300i_COP1_D:
				break;
			case R4300i_COP1_W:
				break;
			case R4300i_COP1_L:
				break;
			default:
				return 1;
			}
			break;
		case R4300i_ANDI:
		case R4300i_ORI:
		case R4300i_XORI:
		case R4300i_LUI:
		case R4300i_ADDI:
		case R4300i_ADDIU:
		case R4300i_SLTI:
		case R4300i_SLTIU:
		case R4300i_DADDI:
		case R4300i_DADDIU:
		case R4300i_LB:
		case R4300i_LH:
		case R4300i_LW:
		case R4300i_LWL:
		case R4300i_LWR:
		case R4300i_LDL:
		case R4300i_LDR:
		case R4300i_LBU:
		case R4300i_LHU:
		case R4300i_LD:
		case R4300i_LWC1:
		case R4300i_LDC1:
			if (tmpRt == 0) { return 0; }
			if (tmpRt == reg1) { return 1; }
			if (tmpRt == reg2) { return 1; }
			break;
		case R4300i_CACHE:
			break;
		case R4300i_SB:
			break;
		case R4300i_SH:
			break;
		case R4300i_SW:
			break;
		case R4300i_SWR:
			break;
		case R4300i_SWL:
			break;
		case R4300i_SWC1:
			break;
		case R4300i_SDC1:
			break;
		case R4300i_SD:
			break;
		default:
			return 1;
		}
		return 0;
	}

	private int compareUnsignedLongs(long a, long b)
	{
		if (a == b) return 0;
		if (((a >> 32) & 0xFFFFFFFFL) < ((b >> 32) & 0xFFFFFFFFL)) return -1;
		if (((a >> 32) & 0xFFFFFFFFL) > ((b >> 32) & 0xFFFFFFFFL)) return 1;
		if (((a >> 32) & 0xFFFFFFFFL) == ((b >> 32) & 0xFFFFFFFFL)
				&& (a & 0xFFFFFFFFL) < (b & 0xFFFFFFFFL)) return -1;
		return 1;
	}

	private void buildOps()
	{
		r4300i_Opcode = new OpCode[64];
		for (int i = 0; i < 64; i++)
			r4300i_Opcode[i] = R4300i_UnknownOpcode;
		r4300i_Opcode[0] = R4300i_opcode_SPECIAL;
		r4300i_Opcode[1] = R4300i_opcode_REGIMM;
		r4300i_Opcode[2] = r4300i_J;
		r4300i_Opcode[3] = r4300i_JAL;
		r4300i_Opcode[4] = r4300i_BEQ;
		r4300i_Opcode[5] = r4300i_BNE;
		r4300i_Opcode[6] = r4300i_BLEZ;
		r4300i_Opcode[7] = r4300i_BGTZ;
		r4300i_Opcode[8] = r4300i_ADDI;
		r4300i_Opcode[9] = r4300i_ADDIU;
		r4300i_Opcode[10] = r4300i_SLTI;
		r4300i_Opcode[11] = r4300i_SLTIU;
		r4300i_Opcode[12] = r4300i_ANDI;
		r4300i_Opcode[13] = r4300i_ORI;
		r4300i_Opcode[14] = r4300i_XORI;
		r4300i_Opcode[15] = r4300i_LUI;
		r4300i_Opcode[16] = R4300i_opcode_COP0;
		r4300i_Opcode[17] = R4300i_opcode_COP1;
		r4300i_Opcode[20] = r4300i_BEQL;
		r4300i_Opcode[21] = r4300i_BNEL;
		r4300i_Opcode[22] = r4300i_BLEZL;
		r4300i_Opcode[23] = r4300i_BGTZL;
		r4300i_Opcode[25] = r4300i_DADDIU;
		r4300i_Opcode[26] = r4300i_LDL;
		r4300i_Opcode[27] = r4300i_LDR;
		r4300i_Opcode[32] = r4300i_LB;
		r4300i_Opcode[33] = r4300i_LH;
		r4300i_Opcode[34] = r4300i_LWL;
		r4300i_Opcode[35] = r4300i_LW;
		r4300i_Opcode[36] = r4300i_LBU;
		r4300i_Opcode[37] = r4300i_LHU;
		r4300i_Opcode[38] = r4300i_LWR;
		r4300i_Opcode[39] = R4300i_UnknownOpcode; // r4300i_LWU;
		r4300i_Opcode[40] = r4300i_SB;
		r4300i_Opcode[41] = r4300i_SH;
		r4300i_Opcode[42] = r4300i_SWL;
		r4300i_Opcode[43] = r4300i_SW;
		r4300i_Opcode[44] = r4300i_SDL;
		r4300i_Opcode[45] = r4300i_SDR;
		r4300i_Opcode[46] = r4300i_SWR;
		r4300i_Opcode[47] = r4300i_CACHE;
		r4300i_Opcode[48] = R4300i_UnknownOpcode; // r4300i_LL;
		r4300i_Opcode[49] = r4300i_LWC1;
		r4300i_Opcode[53] = r4300i_LDC1;
		r4300i_Opcode[55] = r4300i_LD;
		r4300i_Opcode[56] = R4300i_UnknownOpcode; // r4300i_SC;
		r4300i_Opcode[57] = r4300i_SWC1;
		r4300i_Opcode[61] = r4300i_SDC1;
		r4300i_Opcode[63] = r4300i_SD;

		r4300i_Special = new OpCode[64];
		for (int i = 0; i < 64; i++)
			r4300i_Special[i] = R4300i_UnknownOpcode;
		r4300i_Special[0] = r4300i_SPECIAL_SLL; // NOP?
		r4300i_Special[2] = r4300i_SPECIAL_SRL;
		r4300i_Special[3] = r4300i_SPECIAL_SRA;
		r4300i_Special[4] = r4300i_SPECIAL_SLLV;
		r4300i_Special[6] = r4300i_SPECIAL_SRLV;
		r4300i_Special[7] = r4300i_SPECIAL_SRAV;
		r4300i_Special[8] = r4300i_SPECIAL_JR;
		r4300i_Special[9] = r4300i_SPECIAL_JALR;
		r4300i_Special[12] = r4300i_SPECIAL_SYSCALL;
		r4300i_Special[13] = R4300i_UnknownOpcode; // r4300i_SPECIAL_BREAK; // ??
		r4300i_Special[15] = R4300i_UnknownOpcode; // r4300i_SPECIAL_SYNC;
		r4300i_Special[16] = r4300i_SPECIAL_MFHI;
		r4300i_Special[17] = r4300i_SPECIAL_MTHI;
		r4300i_Special[18] = r4300i_SPECIAL_MFLO;
		r4300i_Special[19] = r4300i_SPECIAL_MTLO;
		r4300i_Special[20] = r4300i_SPECIAL_DSLLV;
		r4300i_Special[22] = r4300i_SPECIAL_DSRLV;
		r4300i_Special[23] = R4300i_UnknownOpcode; // r4300i_SPECIAL_DSRAV;
		r4300i_Special[24] = r4300i_SPECIAL_MULT;
		r4300i_Special[25] = r4300i_SPECIAL_MULTU;
		r4300i_Special[26] = r4300i_SPECIAL_DIV;
		r4300i_Special[27] = r4300i_SPECIAL_DIVU;
		r4300i_Special[28] = R4300i_UnknownOpcode; // r4300i_SPECIAL_DMULT;
		r4300i_Special[29] = r4300i_SPECIAL_DMULTU;
		r4300i_Special[30] = r4300i_SPECIAL_DDIV;
		r4300i_Special[31] = r4300i_SPECIAL_DDIVU;
		r4300i_Special[32] = r4300i_SPECIAL_ADD;
		r4300i_Special[33] = r4300i_SPECIAL_ADDU;
		r4300i_Special[34] = r4300i_SPECIAL_SUB;
		r4300i_Special[35] = r4300i_SPECIAL_SUBU;
		r4300i_Special[36] = r4300i_SPECIAL_AND;
		r4300i_Special[37] = r4300i_SPECIAL_OR;
		r4300i_Special[38] = r4300i_SPECIAL_XOR;
		r4300i_Special[39] = r4300i_SPECIAL_NOR;
		r4300i_Special[42] = r4300i_SPECIAL_SLT;
		r4300i_Special[43] = r4300i_SPECIAL_SLTU;
		r4300i_Special[44] = R4300i_UnknownOpcode; // r4300i_SPECIAL_DADD;
		r4300i_Special[45] = r4300i_SPECIAL_DADDU;
		r4300i_Special[46] = R4300i_UnknownOpcode; // r4300i_SPECIAL_DSUB;
		r4300i_Special[47] = R4300i_UnknownOpcode; // r4300i_SPECIAL_DSUBU;
		r4300i_Special[52] = R4300i_UnknownOpcode; // r4300i_SPECIAL_TEQ;
		r4300i_Special[56] = r4300i_SPECIAL_DSLL;
		r4300i_Special[58] = r4300i_SPECIAL_DSRL;
		r4300i_Special[59] = R4300i_UnknownOpcode; // r4300i_SPECIAL_DSRA;
		r4300i_Special[60] = r4300i_SPECIAL_DSLL32;
		r4300i_Special[62] = r4300i_SPECIAL_DSRL32;
		r4300i_Special[63] = r4300i_SPECIAL_DSRA32;

		r4300i_Regimm = new OpCode[32];
		for (int i = 0; i < 32; i++)
			r4300i_Regimm[i] = R4300i_UnknownOpcode;
		r4300i_Regimm[0] = r4300i_REGIMM_BLTZ;
		r4300i_Regimm[1] = r4300i_REGIMM_BGEZ;
		r4300i_Regimm[2] = r4300i_REGIMM_BLTZL;
		r4300i_Regimm[3] = r4300i_REGIMM_BGEZL;
		r4300i_Regimm[16] = r4300i_REGIMM_BLTZAL;
		r4300i_Regimm[17] = r4300i_REGIMM_BGEZAL;
	}

	/************************* OpCode functions *************************/

	protected OpCode R4300i_opcode_SPECIAL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			r4300i_Special[instr & 0x3F].exec(instr, unused);
		}
	};

	protected OpCode R4300i_opcode_REGIMM = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			r4300i_Regimm[(instr >> RT) & 0x1F].exec(instr, unused);
		}
	};

	protected OpCode R4300i_opcode_COP0 = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			cop0.write32bit(COP0_INSTR_REG, instr);
		}
	};

	protected OpCode R4300i_opcode_COP1 = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int status = cop0.read32bit(STATUS_REGISTER);
			if ((status & STATUS_CU1) == 0)
			{
				cop0.write32bit(COP0_EXC_REG, EXC_CPU | CE_COP1);
				return;
			}
			if ((status & STATUS_FR) == mode32)
			{
				mode32 ^= 1;
				cop1.write32bit(COP1_FP_EXT_REG, status & STATUS_FR);
			}
			cop1.write32bit(COP1_INSTR_REG, instr);
		}
	};

	protected OpCode r4300i_J = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			jumpToLocation = (pc & 0xF0000000) + ((instr & 0x3FFFFFF) << 2);
			nextInstruction = DELAY_SLOT;
			testInterpreterJump();
		}
	};

	protected OpCode r4300i_JAL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			jumpToLocation = (pc & 0xF0000000) + ((instr & 0x3FFFFFF) << 2);
			nextInstruction = DELAY_SLOT;
			testInterpreterJump();
			GPR[31] = pc + 8;
		}
	};

	protected OpCode r4300i_BEQ = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] == GPR[(instr >> RT) & 0x1F])
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = DELAY_SLOT;
			}
		}
	};

	protected OpCode r4300i_BNE = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] != GPR[(instr >> RT) & 0x1F])
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = DELAY_SLOT;
			}
		}
	};

	protected OpCode r4300i_BLEZ = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] <= 0L)
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = DELAY_SLOT;
			}
		}
	};

	protected OpCode r4300i_BGTZ = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] > 0L)
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = DELAY_SLOT;
			}
		}
	};

	protected OpCode r4300i_ADDI = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (((instr >> RT) & 0x1F) == 0) return;
			GPR[(instr >> RT) & 0x1F] = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
		}
	};

	protected OpCode r4300i_ADDIU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RT) & 0x1F] = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
		}
	};

	protected OpCode r4300i_SLTI = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] < (long) ((short) instr))
				GPR[(instr >> RT) & 0x1F] = 1L;
			else
				GPR[(instr >> RT) & 0x1F] = 0L;
		}
	};

	protected OpCode r4300i_SLTIU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (compareUnsignedLongs(GPR[(instr >> RS) & 0x1F], (long) ((short) instr)) < 0)
				GPR[(instr >> RT) & 0x1F] = 1L;
			else
				GPR[(instr >> RT) & 0x1F] = 0L;
		}
	};

	protected OpCode r4300i_ANDI = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RT) & 0x1F] = GPR[(instr >> RS) & 0x1F] & (instr & 0xFFFF);
		}
	};

	protected OpCode r4300i_ORI = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RT) & 0x1F] = GPR[(instr >> RS) & 0x1F] | (instr & 0xFFFF);
		}
	};

	protected OpCode r4300i_XORI = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RT) & 0x1F] = GPR[(instr >> RS) & 0x1F] ^ (instr & 0xFFFF);
		}
	};

	protected OpCode r4300i_LUI = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (((instr >> RT) & 0x1F) == 0) return;
			GPR[(instr >> RT) & 0x1F] = (instr & 0xFFFF) << 16;
		}
	};

	protected OpCode r4300i_BEQL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] == GPR[(instr >> RT) & 0x1F])
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = JUMP;
			}
		}
	};

	protected OpCode r4300i_BNEL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] != GPR[(instr >> RT) & 0x1F])
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = JUMP;
			}
		}
	};

	protected OpCode r4300i_BLEZL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] <= 0L)
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = JUMP;
			}
		}
	};

	protected OpCode r4300i_BGTZL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] > 0L)
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = JUMP;
			}
		}
	};

	protected OpCode r4300i_DADDIU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RT) & 0x1F] = GPR[(instr >> RS) & 0x1F] + (long) ((short) instr);
		}
	};

	protected OpCode r4300i_LDL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			tmpDouble = mi64bit.read64bit(mmu.read32bit(addr) & ~7);
			GPR[(instr >> RT) & 0x1F] = (GPR[(instr >> RT) & 0x1F] & LDL_MASK[addr & 7])
					+ (tmpDouble << LDL_SHIFT[addr & 7]);
		}
	};

	protected OpCode r4300i_LDR = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			tmpDouble = mi64bit.read64bit(mmu.read32bit(addr) & ~7);
			GPR[(instr >> RT) & 0x1F] = (GPR[(instr >> RT) & 0x1F] & LDR_MASK[addr & 7])
					+ (tmpDouble >> LDR_SHIFT[addr & 7]);
		}
	};

	protected OpCode r4300i_LB = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (((instr >> RT) & 0x1F) == 0) {
			return;
			}
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			GPR[(instr >> RT) & 0x1F] = mi8bit.read8bit(mmu.read32bit(addr));
		}
	};

	protected OpCode r4300i_LH = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 1) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_RADE);
				return;
			}
			GPR[(instr >> RT) & 0x1F] = mi16bit.read16bit(mmu.read32bit(addr));
		}
	};

	protected OpCode r4300i_LWL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			tmpWord = mi32bit.read32bit(mmu.read32bit(addr) & ~3);
			GPR[(instr >> RT) & 0x1F] = ((int) GPR[(instr >> RT) & 0x1F] & LWL_MASK[addr & 3])
					+ (tmpWord << LWL_SHIFT[addr & 3]);
		}
	};

	protected OpCode r4300i_LW = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (((instr >> RT) & 0x1F) == 0) {
			return;
			}
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 3) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_RADE);
				return;
			}
			GPR[(instr >> RT) & 0x1F] = mi32bit.read32bit(mmu.read32bit(addr));
		}
	};

	protected OpCode r4300i_LBU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			GPR[(instr >> RT) & 0x1F] = mi8bit.read8bit(mmu.read32bit(addr)) & 0xFFL;
		}
	};

	protected OpCode r4300i_LHU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 1) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_RADE);
				return;
			}
			GPR[(instr >> RT) & 0x1F] = mi16bit.read16bit(mmu.read32bit(addr)) & 0xFFFFL;
		}
	};

	protected OpCode r4300i_LWR = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			tmpWord = mi32bit.read32bit(mmu.read32bit(addr) & ~3);
			GPR[(instr >> RT) & 0x1F] = ((int) GPR[(instr >> RT) & 0x1F] & LWR_MASK[addr & 3])
					+ (tmpWord >> LWR_SHIFT[addr & 3]);
		}
	};

	protected OpCode r4300i_LWU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (((instr >> RT) & 0x1F) == 0) {
			return;
			}
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 3) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_RADE);
				return;
			}
			GPR[(instr >> RT) & 0x1F] = mi32bit.read32bit(mmu.read32bit(addr)) & 0xFFFFFFFFL;
		}
	};

	protected OpCode r4300i_SB = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if (cacheInstructions)
			{
				int pAddr = mmu.read32bit(addr);
				if (pAddr < 0x400000)
					cachedOpcodes[pAddr >>> 2].cached = false;
				else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
					cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
				mi8bit.write8bit(pAddr, (byte) GPR[(instr >> RT) & 0x1F]);
			}
			else
			{
				mi8bit.write8bit(mmu.read32bit(addr), (byte) GPR[(instr >> RT) & 0x1F]);
			}
		}
	};

	protected OpCode r4300i_SH = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 1) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_WADE);
				return;
			}
			if (cacheInstructions)
			{
				int pAddr = mmu.read32bit(addr);
				if (pAddr < 0x400000)
					cachedOpcodes[pAddr >>> 2].cached = false;
				else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
					cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
				mi16bit.write16bit(pAddr, (short) GPR[(instr >> RT) & 0x1F]);
			}
			else
			{
				mi16bit.write16bit(mmu.read32bit(addr), (short) GPR[(instr >> RT) & 0x1F]);
			}
		}
	};

	protected OpCode r4300i_SWL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			tmpWord = (mi32bit.read32bit(mmu.read32bit(addr) & ~3) & SWL_MASK[addr & 3])
					+ ((int) GPR[(instr >> RT) & 0x1F] >>> SWL_SHIFT[addr & 3]);
			if (cacheInstructions)
			{
				int pAddr = mmu.read32bit(addr) & ~3;
				if (pAddr < 0x400000)
					cachedOpcodes[pAddr >>> 2].cached = false;
				else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
					cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
				mi32bit.write32bit(pAddr, tmpWord);
			}
			else
			{
				mi32bit.write32bit(mmu.read32bit(addr) & ~3, tmpWord);
			}
		}
	};

	protected OpCode r4300i_SW = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 3) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_WADE);
				return;
			}
			if (cacheInstructions)
			{
				int pAddr = mmu.read32bit(addr);
				if (pAddr < 0x400000)
					cachedOpcodes[pAddr >>> 2].cached = false;
				else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
					cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
				mi32bit.write32bit(pAddr, (int) GPR[(instr >> RT) & 0x1F]);
			}
			else
			{
				mi32bit.write32bit(mmu.read32bit(addr), (int) GPR[(instr >> RT) & 0x1F]);
			}
		}
	};

	protected OpCode r4300i_SDL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			tmpDouble = (mi64bit.read64bit(mmu.read32bit(addr) & ~7) & SDL_MASK[addr & 7])
					+ (GPR[(instr >> RT) & 0x1F] >> SDL_SHIFT[addr & 7]);
			if (cacheInstructions)
			{
				int pAddr = mmu.read32bit(addr) & ~7;
				if (pAddr < 0x400000)
				{
					cachedOpcodes[pAddr >>> 2].cached = false;
					cachedOpcodes[(pAddr + 4) >>> 2].cached = false;
				}
				else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
				{
					cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
					cachedOpcodes[((pAddr + 4) - 0x3C00000) >>> 2].cached = false;
				}
				mi64bit.write64bit(pAddr, tmpDouble);
			}
			else
			{
				mi64bit.write64bit(mmu.read32bit(addr) & ~7, tmpDouble);
			}
		}
	};

	protected OpCode r4300i_SDR = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			tmpDouble = (mi64bit.read64bit(mmu.read32bit(addr) & ~7) & SDR_MASK[addr & 7])
					+ (GPR[(instr >> RT) & 0x1F] << SDR_SHIFT[addr & 7]);
			if (cacheInstructions)
			{
				int pAddr = mmu.read32bit(addr) & ~7;
				if (pAddr < 0x400000)
				{
					cachedOpcodes[pAddr >>> 2].cached = false;
					cachedOpcodes[(pAddr + 4) >>> 2].cached = false;
				}
				else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
				{
					cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
					cachedOpcodes[((pAddr + 4) - 0x3C00000) >>> 2].cached = false;
				}
				mi64bit.write64bit(pAddr, tmpDouble);
			}
			else
			{
				mi64bit.write64bit(mmu.read32bit(addr) & ~7, tmpDouble);
			}
		}
	};

	protected OpCode r4300i_SWR = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			tmpWord = (mi32bit.read32bit(mmu.read32bit(addr) & ~3) & SWR_MASK[addr & 3])
					+ ((int) GPR[(instr >> RT) & 0x1F] << SWR_SHIFT[addr & 3]);
			if (cacheInstructions)
			{
				int pAddr = mmu.read32bit(addr) & ~3;
				if (pAddr < 0x400000)
					cachedOpcodes[pAddr >>> 2].cached = false;
				else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
					cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
				mi32bit.write32bit(pAddr, tmpWord);
			}
			else
			{
				mi32bit.write32bit(mmu.read32bit(addr) & ~3, tmpWord);
			}
		}
	};

	protected OpCode r4300i_CACHE = new OpCode()
	{
		public void exec(int instr, int unused)
		{
		}
	};

	protected OpCode r4300i_LL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (((instr >> RT) & 0x1F) == 0) {
			return;
			}
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 3) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_RADE);
				return;
			}
			GPR[(instr >> RT) & 0x1F] = mi32bit.read32bit(mmu.read32bit(addr));
			llBit = 1;
			llAddr = addr;
			llAddr = mmu.read32bit(addr);
		}
	};

	protected OpCode r4300i_LWC1 = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if ((cop0.read32bit(STATUS_REGISTER) & STATUS_CU1) == 0)
			{
				cop0.write32bit(COP0_EXC_REG, EXC_CPU | CE_COP1);
				return;
			}
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 3) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_RADE);
				return;
			}
			cop1.write32bit((instr >> RT) & 0x1F, mi32bit.read32bit(mmu.read32bit(addr)));
		}
	};

	protected OpCode r4300i_SC = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 3) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_WADE);
				return;
			}
			if (llBit == 1)
			{
				if (cacheInstructions)
				{
					int pAddr = mmu.read32bit(addr);
					if (pAddr < 0x400000)
						cachedOpcodes[pAddr >>> 2].cached = false;
					else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
						cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
					mi32bit.write32bit(pAddr, (int) GPR[(instr >> RT) & 0x1F]);
				}
				else
				{
					mi32bit.write32bit(mmu.read32bit(addr), (int) GPR[(instr >> RT) & 0x1F]);
				}
			}
			GPR[(instr >> RT) & 0x1F] = (llBit & 0x00000000FFFFFFFFL) | (GPR[(instr >> RT) & 0x1F] & 0xFFFFFFFF00000000L);
		}
	};

	protected OpCode r4300i_LD = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 7) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_RADE);
				return;
			}
			GPR[(instr >> RT) & 0x1F] = mi64bit.read64bit(mmu.read32bit(addr));
		}
	};

	protected OpCode r4300i_LDC1 = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if ((cop0.read32bit(STATUS_REGISTER) & STATUS_CU1) == 0)
			{
				cop0.write32bit(COP0_EXC_REG, EXC_CPU | CE_COP1);
				return;
			}
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 7) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_RADE);
				return;
			}
			((Bus64bit) cop1).write64bit((instr >> RT) & 0x1F, mi64bit.read64bit(mmu.read32bit(addr)));
		}
	};

	protected OpCode r4300i_SWC1 = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if ((cop0.read32bit(STATUS_REGISTER) & STATUS_CU1) == 0)
			{
				cop0.write32bit(COP0_EXC_REG, EXC_CPU | CE_COP1);
				return;
			}
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 3) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_WADE);
				return;
			}
			if (cacheInstructions)
			{
				int pAddr = mmu.read32bit(addr);
				if (pAddr < 0x400000)
					cachedOpcodes[pAddr >>> 2].cached = false;
				else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
					cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
				mi32bit.write32bit(pAddr, cop1.read32bit((instr >> RT) & 0x1F));
			}
			else
			{
				mi32bit.write32bit(mmu.read32bit(addr), cop1.read32bit((instr >> RT) & 0x1F));
			}
		}
	};

	protected OpCode r4300i_SDC1 = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if ((cop0.read32bit(STATUS_REGISTER) & STATUS_CU1) == 0)
			{
				cop0.write32bit(COP0_EXC_REG, EXC_CPU | CE_COP1);
				return;
			}
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 7) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_WADE);
				return;
			}
			if (cacheInstructions)
			{
				int pAddr = mmu.read32bit(addr);
				if (pAddr < 0x400000)
				{
					cachedOpcodes[pAddr >>> 2].cached = false;
					cachedOpcodes[(pAddr + 4) >>> 2].cached = false;
				}
				else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
				{
					cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
					cachedOpcodes[((pAddr + 4) - 0x3C00000) >>> 2].cached = false;
				}
				mi64bit.write64bit(pAddr, ((Bus64bit) cop1).read64bit((instr >> RT) & 0x1F));
			}
			else
			{
				mi64bit.write64bit(mmu.read32bit(addr), ((Bus64bit) cop1).read64bit((instr >> RT) & 0x1F));
			}
		}
	};

	protected OpCode r4300i_SD = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			int addr = (int) GPR[(instr >> RS) & 0x1F] + (short) instr;
			if ((addr & 7) != 0)
			{
				cop0.write32bit(COP0_ADDRESS_REG, addr);
				cop0.write32bit(COP0_EXC_REG, EXC_WADE);
				return;
			}
			if (cacheInstructions)
			{
				int pAddr = mmu.read32bit(addr);
				if (pAddr < 0x400000)
				{
					cachedOpcodes[pAddr >>> 2].cached = false;
					cachedOpcodes[(pAddr + 4) >>> 2].cached = false;
				}
				else if (((pAddr - 0x3C00000) >>> 2) < cachedOpcodes.length)
				{
					cachedOpcodes[(pAddr - 0x3C00000) >>> 2].cached = false;
					cachedOpcodes[((pAddr + 4) - 0x3C00000) >>> 2].cached = false;
				}
				mi64bit.write64bit(pAddr, GPR[(instr >> RT) & 0x1F]);
			}
			else
			{
				mi64bit.write64bit(mmu.read32bit(addr), GPR[(instr >> RT) & 0x1F]);
			}
		}
	};

	/********************** R4300i OpCodes: Special **********************/

	protected OpCode r4300i_SPECIAL_SLL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = (int) GPR[(instr >> RT) & 0x1F] << ((instr >> SA) & 0x1F);
		}
	};

	protected OpCode r4300i_SPECIAL_SRL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = (int) GPR[(instr >> RT) & 0x1F] >>> ((instr >> SA) & 0x1F);
		}
	};

	protected OpCode r4300i_SPECIAL_SRA = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = (int) GPR[(instr >> RT) & 0x1F] >> ((instr >> SA) & 0x1F);
		}
	};

	protected OpCode r4300i_SPECIAL_SLLV = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (((instr >> RD) & 0x1F) == 0) return;
			GPR[(instr >> RD) & 0x1F] = (int) GPR[(instr >> RT) & 0x1F] << (GPR[(instr >> RS) & 0x1F] & 0x1F);
		}
	};

	protected OpCode r4300i_SPECIAL_SRLV = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = (int) GPR[(instr >> RT) & 0x1F] >>> (GPR[(instr >> RS) & 0x1F] & 0x1F);
		}
	};

	protected OpCode r4300i_SPECIAL_SRAV = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = (int) GPR[(instr >> RT) & 0x1F] >> (GPR[(instr >> RS) & 0x1F] & 0x1F);
		}
	};

	protected OpCode r4300i_SPECIAL_JR = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			nextInstruction = DELAY_SLOT;
			jumpToLocation = (int) GPR[(instr >> RS) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_JALR = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			nextInstruction = DELAY_SLOT;
			jumpToLocation = (int) GPR[(instr >> RS) & 0x1F];
			GPR[(instr >> RD) & 0x1F] = pc + 8;
		}
	};

	protected OpCode r4300i_SPECIAL_SYSCALL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			cop0.write32bit(COP0_EXC_REG, EXC_SYSCALL);
		}
	};

	protected OpCode r4300i_SPECIAL_BREAK = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			cop0.write32bit(COP0_EXC_REG, EXC_BREAK);
		}
	};

	protected OpCode r4300i_SPECIAL_SYNC = new OpCode()
	{
		public void exec(int instr, int unused)
		{
		}
	};

	protected OpCode r4300i_SPECIAL_MFHI = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = HI;
		}
	};

	protected OpCode r4300i_SPECIAL_MTHI = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			HI = GPR[(instr >> RS) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_MFLO = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = LO;
		}
	};

	protected OpCode r4300i_SPECIAL_MTLO = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			LO = GPR[(instr >> RS) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_DSLLV = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RT) & 0x1F] << (GPR[(instr >> RS) & 0x1F] & 0x3F);
		}
	};

	protected OpCode r4300i_SPECIAL_DSRLV = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RT) & 0x1F] >>> (GPR[(instr >> RS) & 0x1F] & 0x3F);
		}
	};

	protected OpCode r4300i_SPECIAL_DSRAV = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RT) & 0x1F] >> (GPR[(instr >> RS) & 0x1F] & 0x3F);
		}
	};

	protected OpCode r4300i_SPECIAL_MULT = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			HI = (long) ((int) GPR[(instr >> RS) & 0x1F]) * (long) ((int) GPR[(instr >> RT) & 0x1F]);
			LO = (int) HI;
			HI = (int) (HI >> 32);
		}
	};

	protected OpCode r4300i_SPECIAL_MULTU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			HI = (GPR[(instr >> RS) & 0x1F] & 0xFFFFFFFFL) * (GPR[(instr >> RT) & 0x1F] & 0xFFFFFFFFL);
			LO = (int) HI;
			HI = (int) (HI >> 32);
		}
	};

	protected OpCode r4300i_SPECIAL_DIV = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			LO = (int) GPR[(instr >> RS) & 0x1F] / (int) GPR[(instr >> RT) & 0x1F];
			HI = (int) GPR[(instr >> RS) & 0x1F] % (int) GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_DIVU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			LO = (GPR[(instr >> RS) & 0x1F] & 0xFFFFFFFFL) / (GPR[(instr >> RT) & 0x1F] & 0xFFFFFFFFL);
			HI = (GPR[(instr >> RS) & 0x1F] & 0xFFFFFFFFL) % (GPR[(instr >> RT) & 0x1F] & 0xFFFFFFFFL);
		}
	};

	protected OpCode r4300i_SPECIAL_DMULT = new OpCode()
	{
		public void exec(int instr, int unused)
		{
		}
	};

	protected OpCode r4300i_SPECIAL_DMULTU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			LO = (GPR[(instr >> RS) & 0x1F] & 0xFFFFFFFFL) * (GPR[(instr >> RT) & 0x1F] & 0xFFFFFFFFL);
			tmpDmultu[0] = (GPR[(instr >> RS) & 0x1F] >>> 32) * (GPR[(instr >> RT) & 0x1F] & 0xFFFFFFFFL);
			tmpDmultu[1] = (GPR[(instr >> RS) & 0x1F] & 0xFFFFFFFFL) * (GPR[(instr >> RT) & 0x1F] >>> 32);
			HI = (GPR[(instr >> RS) & 0x1F] >>> 32) * (GPR[(instr >> RT) & 0x1F] >>> 32);
			tmpDmultu[2] = (LO >>> 32) + (tmpDmultu[0] & 0xFFFFFFFFL) + (tmpDmultu[1] & 0xFFFFFFFFL);
			LO += ((tmpDmultu[0] & 0xFFFFFFFFL) + (tmpDmultu[1] & 0xFFFFFFFFL)) << 32;
			HI += (tmpDmultu[0] >>> 32) + (tmpDmultu[1] >>> 32) + (tmpDmultu[2] >>> 32);
		}
	};

	protected OpCode r4300i_SPECIAL_DDIV = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			LO = GPR[(instr >> RS) & 0x1F] / GPR[(instr >> RT) & 0x1F];
			HI = GPR[(instr >> RS) & 0x1F] % GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_DDIVU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			LO = GPR[(instr >> RS) & 0x1F] / GPR[(instr >> RT) & 0x1F];
			HI = GPR[(instr >> RS) & 0x1F] % GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_ADD = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = (int) GPR[(instr >> RS) & 0x1F] + (int) GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_ADDU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = (int) GPR[(instr >> RS) & 0x1F] + (int) GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_SUB = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = (int) GPR[(instr >> RS) & 0x1F] - (int) GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_SUBU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = (int) GPR[(instr >> RS) & 0x1F] - (int) GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_AND = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RS) & 0x1F] & GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_OR = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RS) & 0x1F] | GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_XOR = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RS) & 0x1F] ^ GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_NOR = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = ~(GPR[(instr >> RS) & 0x1F] | GPR[(instr >> RT) & 0x1F]);
		}
	};

	protected OpCode r4300i_SPECIAL_SLT = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			// signed comparison
			if (GPR[(instr >> RS) & 0x1F] < GPR[(instr >> RT) & 0x1F])
				GPR[(instr >> RD) & 0x1F] = 1L;
			else
				GPR[(instr >> RD) & 0x1F] = 0L;
		}
	};

	protected OpCode r4300i_SPECIAL_SLTU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (compareUnsignedLongs(GPR[(instr >> RS) & 0x1F], GPR[(instr >> RT) & 0x1F]) < 0)
				GPR[(instr >> RD) & 0x1F] = 1L;
			else
				GPR[(instr >> RD) & 0x1F] = 0L;
		}
	};

	protected OpCode r4300i_SPECIAL_DADD = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RS) & 0x1F] + GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_DADDU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RS) & 0x1F] + GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_DSUB = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RS) & 0x1F] - GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_DSUBU = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RS) & 0x1F] - GPR[(instr >> RT) & 0x1F];
		}
	};

	protected OpCode r4300i_SPECIAL_TEQ = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			// if (GPR[(inst>>RS)&0x1F] == GPR[(inst>>RT)&0x1F])
			// System.err.printf("Should trap this ???\n");
		}
	};

	protected OpCode r4300i_SPECIAL_DSLL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RT) & 0x1F] << ((instr >> SA) & 0x1F);
		}
	};

	protected OpCode r4300i_SPECIAL_DSRL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RT) & 0x1F] >>> ((instr >> SA) & 0x1F);
		}
	};

	protected OpCode r4300i_SPECIAL_DSRA = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RT) & 0x1F] >> ((instr >> SA) & 0x1F);
		}
	};

	protected OpCode r4300i_SPECIAL_DSLL32 = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RT) & 0x1F] << (((instr >> SA) & 0x1F) + 32);
		}
	};

	protected OpCode r4300i_SPECIAL_DSRL32 = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RT) & 0x1F] >>> (((instr >> SA) & 0x1F) + 32);
		}
	};

	protected OpCode r4300i_SPECIAL_DSRA32 = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			GPR[(instr >> RD) & 0x1F] = GPR[(instr >> RT) & 0x1F] >> (((instr >> SA) & 0x1F) + 32);
		}
	};

	/********************** R4300i OpCodes: RegImm **********************/

	protected OpCode r4300i_REGIMM_BLTZ = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] < 0L)
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = DELAY_SLOT;
			}
		}
	};

	protected OpCode r4300i_REGIMM_BGEZ = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] >= 0L)
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = DELAY_SLOT;
			}
		}
	};

	protected OpCode r4300i_REGIMM_BLTZL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] < 0L)
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = JUMP;
			}
		}
	};

	protected OpCode r4300i_REGIMM_BGEZL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] >= 0L)
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = JUMP;
			}
		}
	};

	protected OpCode r4300i_REGIMM_BLTZAL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] < 0L)
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = DELAY_SLOT;
			}
			GPR[31] = pc + 8;
		}
	};

	protected OpCode r4300i_REGIMM_BGEZAL = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			if (GPR[(instr >> RS) & 0x1F] >= 0L)
			{
				jumpToLocation = pc + (((short) instr) << 2) + 4;
				nextInstruction = DELAY_SLOT;
				testInterpreterJump();
			}
			else
			{
				jumpToLocation = pc + 8;
				nextInstruction = DELAY_SLOT;
			}
			GPR[31] = pc + 8;
		}
	};

	/************************** Other functions **************************/

	protected OpCode R4300i_UnknownOpcode = new OpCode()
	{
		public void exec(int instr, int unused)
		{
			System.err.printf("PC:%X ,Unhandled r4300i OpCode:%X\n", pc, instr);
			System.exit(0);
		}
	};
}
