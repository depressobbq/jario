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

import jario.hardware.Bus32bit;
import jario.hardware.Bus64bit;
import jario.hardware.Hardware;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Registers:
 * 0 - 31: (r/w) General Purpose Floating Point Registers (32/64 bit)
 * Accessed with load/store Word and load/store DoubleWord
 * 64: (r/w) FP_EXT_REG
 * 0 = General Purpose FP Registers are 32 bit
 * 1 = General Purpose FP Registers are extended to 64 bit
 * 65: (r/w) INSTR_REG
 * load = Returns the last instruction executed
 * store = Executes an instruction
 * load/store Byte and load/store HalfWord are not supported
 */
public class Fpu implements Hardware, Bus32bit, Bus64bit
{
	// FPCR Registers
	protected static final int REVISION_REGISTER = 0;
	protected static final int FSTATUS_REGISTER = 31;

	protected static final int FPCSR_FS = 0x01000000; /* flush denorm to zero */
	protected static final int FPCSR_C = 0x00800000; /* condition bit */
	protected static final int FPCSR_CE = 0x00020000; /* cause: unimplemented operation */
	protected static final int FPCSR_CV = 0x00010000; /* cause: invalid operation */
	protected static final int FPCSR_CZ = 0x00008000; /* cause: division by zero */
	protected static final int FPCSR_CO = 0x00004000; /* cause: overflow */
	protected static final int FPCSR_CU = 0x00002000; /* cause: underflow */
	protected static final int FPCSR_CI = 0x00001000; /* cause: inexact operation */
	protected static final int FPCSR_EV = 0x00000800; /* enable: invalid operation */
	protected static final int FPCSR_EZ = 0x00000400; /* enable: division by zero */
	protected static final int FPCSR_EO = 0x00000200; /* enable: overflow */
	protected static final int FPCSR_EU = 0x00000100; /* enable: underflow */
	protected static final int FPCSR_EI = 0x00000080; /* enable: inexact operation */
	protected static final int FPCSR_FV = 0x00000040; /* flag: invalid operation */
	protected static final int FPCSR_FZ = 0x00000020; /* flag: division by zero */
	protected static final int FPCSR_FO = 0x00000010; /* flag: overflow */
	protected static final int FPCSR_FU = 0x00000008; /* flag: underflow */
	protected static final int FPCSR_FI = 0x00000004; /* flag: inexact operation */
	protected static final int FPCSR_RM_MASK = 0x00000003; /* rounding mode mask */
	protected static final int FPCSR_RM_RN = 0x00000000; /* round to nearest */
	protected static final int FPCSR_RM_RZ = 0x00000001; /* round to zero */
	protected static final int FPCSR_RM_RP = 0x00000002; /* round to positive infinity */
	protected static final int FPCSR_RM_RM = 0x00000003; /* round to negative infinity */

	// CPU Registers
	protected static final int PC_REGISTER = 32;
	protected static final int DELAY_SLOT_REGISTER = 40;

	protected static final int FMT = 21;
	protected static final int FT = 16;
	protected static final int FS = 11;
	protected static final int FD = 06;

	protected static final int R4300I_COP1 = 0;
	protected static final int R4300I_COP1_BC = 1;
	protected static final int R4300I_COP1_S = 2;
	protected static final int R4300I_COP1_D = 3;
	protected static final int R4300I_COP1_W = 4;
	protected static final int R4300I_COP1_L = 5;

	protected static final int RC_NEAR = 0;
	protected static final int RC_CHOP = 1;
	protected static final int RC_UP = 2;
	protected static final int RC_DOWN = 3;

	protected RoundingMode[] roundingMode =
	{
			RoundingMode.HALF_EVEN,
			RoundingMode.DOWN,
			RoundingMode.CEILING,
			RoundingMode.FLOOR
	};
	protected MathContext[] floatContext =
	{
			MathContext.DECIMAL32,
			new MathContext(7, RoundingMode.DOWN),
			new MathContext(7, RoundingMode.CEILING),
			new MathContext(7, RoundingMode.FLOOR)
	};

	protected class MipsDword
	{
		public long DW;

		public int getW(int index)
		{
			switch (mode32 ? index & 1 : 0)
			{
			case 0:
				return (int) DW;
			case 1:
				return (int) (DW >> 32);
			default:
				return 0;
			}
		}

		public void setW(int index, int w)
		{
			switch (mode32 ? index & 1 : 0)
			{
			case 0:
				DW = ((((long) w)) & 0x00000000FFFFFFFFL) | (DW & 0xFFFFFFFF00000000L);
				break;
			case 1:
				DW = ((((long) w) << 32) & 0xFFFFFFFF00000000L) | (DW & 0x00000000FFFFFFFFL);
				break;
			default:
				return;
			}
		}

		public float getF(int index)
		{
			return Float.intBitsToFloat((int) DW);
		}

		public void setF(int index, float f)
		{
			DW = (((long) Float.floatToIntBits(f))) & 0x00000000FFFFFFFFL;
		}

		public double getD()
		{
			return Double.longBitsToDouble(DW);
		}

		public void setD(double d)
		{
			DW = Double.doubleToLongBits(d);
		}
	}

	public static interface OpCode
	{
		public void exec(int inst1, int inst2);
	}

	protected OpCode[] r4300i_CoP1;
	protected OpCode[] r4300i_CoP1_BC;
	protected OpCode[] r4300i_CoP1_S;
	protected OpCode[] r4300i_CoP1_D;
	protected OpCode[] r4300i_CoP1_W;
	protected OpCode[] r4300i_CoP1_L;

	protected int[] FPCR;
	protected MipsDword[] FPR;
	protected boolean mode32;
	protected int instruction;
	protected int roundingModel;

	private MipsDword[] tmpFPR;

	protected Bus32bit bus0;
	protected Bus64bit bus0DW;

	public Fpu()
	{
		FPCR = new int[32];
		FPCR[REVISION_REGISTER] = 0x00000511;
		FPR = new MipsDword[32];
		tmpFPR = new MipsDword[16];
		for (int i = 0; i < FPR.length; i++)
			FPR[i] = new MipsDword();
		roundingModel = RC_NEAR;
		buildOps();
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			bus0 = (Bus32bit) bus;
			bus0DW = (Bus64bit) bus;
			break;
		}
	}

	@Override
	public void reset()
	{
		FPCR = new int[32];
		FPCR[REVISION_REGISTER] = 0x00000511;
		FPR = new MipsDword[32];
		tmpFPR = new MipsDword[16];
		for (int i = 0; i < FPR.length; i++)
			FPR[i] = new MipsDword();
		mode32 = false;
		instruction = 0;
		roundingModel = RC_NEAR;
	}

	@Override
	public int read32bit(int pAddr)
	{
		if (pAddr < 64)
		{
			return FPR[pAddr].getW(pAddr);
		}
		else
		{
			switch (pAddr)
			{
			case 64:
				return mode32 ? 0 : 1;
			case 65:
				return instruction;
			default:
				return 0;
			}
		}
	}

	@Override
	public long read64bit(int pAddr)
	{
		return FPR[pAddr].DW;
	}

	@Override
	public void write32bit(int pAddr, int value)
	{
		if (pAddr < 64)
		{
			FPR[pAddr].setW(pAddr, value);
		}
		else
		{
			switch (pAddr)
			{
			case 64:
				if (value == 0)
				{
					mode32 = true;
					for (int i = 0; i < 16; i++)
					{
						tmpFPR[i] = FPR[(i << 1) + 1];
						FPR[(i << 1) + 1] = FPR[i << 1];
					}
				}
				else
				{
					mode32 = false;
					for (int i = 0; i < 16; i++)
					{
						FPR[(i << 1) + 1] = tmpFPR[i];
					}
				}
				break;
			case 65:
				instruction = value;
				r4300i_CoP1[(value >> FMT) & 0x1F].exec(value, 0);
				break;
			}
		}
	}

	@Override
	public void write64bit(int pAddr, long value)
	{
		FPR[pAddr].DW = value;
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void buildOps()
	{
		r4300i_CoP1 = new OpCode[32];
		for (int i = 0; i < 32; i++)
			r4300i_CoP1[i] = R4300i_UnknownOpcode;
		r4300i_CoP1[0] = r4300i_COP1_MF;
		r4300i_CoP1[1] = r4300i_COP1_DMF;
		r4300i_CoP1[2] = r4300i_COP1_CF;
		r4300i_CoP1[4] = r4300i_COP1_MT;
		r4300i_CoP1[5] = r4300i_COP1_DMT;
		r4300i_CoP1[6] = r4300i_COP1_CT;
		r4300i_CoP1[8] = R4300i_opcode_COP1_BC;
		r4300i_CoP1[16] = R4300i_opcode_COP1_S;
		r4300i_CoP1[17] = R4300i_opcode_COP1_D;
		r4300i_CoP1[20] = R4300i_opcode_COP1_W;
		r4300i_CoP1[21] = R4300i_opcode_COP1_L;

		r4300i_CoP1_BC = new OpCode[32];
		for (int i = 0; i < 32; i++)
			r4300i_CoP1_BC[i] = R4300i_UnknownOpcode;
		r4300i_CoP1_BC[0] = r4300i_COP1_BCF;
		r4300i_CoP1_BC[1] = r4300i_COP1_BCT;
		r4300i_CoP1_BC[2] = r4300i_COP1_BCFL;
		r4300i_CoP1_BC[3] = r4300i_COP1_BCTL;

		r4300i_CoP1_S = new OpCode[64];
		for (int i = 0; i < 64; i++)
			r4300i_CoP1_S[i] = R4300i_UnknownOpcode;
		r4300i_CoP1_S[0] = r4300i_COP1_S_ADD;
		r4300i_CoP1_S[1] = r4300i_COP1_S_SUB;
		r4300i_CoP1_S[2] = r4300i_COP1_S_MUL;
		r4300i_CoP1_S[3] = r4300i_COP1_S_DIV;
		r4300i_CoP1_S[4] = r4300i_COP1_S_SQRT;
		r4300i_CoP1_S[5] = r4300i_COP1_S_ABS;
		r4300i_CoP1_S[6] = r4300i_COP1_S_MOV;
		r4300i_CoP1_S[7] = r4300i_COP1_S_NEG;
		r4300i_CoP1_S[9] = r4300i_COP1_S_TRUNC_L;
		r4300i_CoP1_S[12] = r4300i_COP1_S_ROUND_W;
		r4300i_CoP1_S[13] = r4300i_COP1_S_TRUNC_W;
		r4300i_CoP1_S[15] = r4300i_COP1_S_FLOOR_W;
		r4300i_CoP1_S[33] = r4300i_COP1_S_CVT_D;
		r4300i_CoP1_S[36] = r4300i_COP1_S_CVT_W;
		r4300i_CoP1_S[37] = r4300i_COP1_S_CVT_L;
		for (int i = 48; i < 64; i++)
			r4300i_CoP1_S[i] = r4300i_COP1_S_CMP;

		r4300i_CoP1_D = new OpCode[64];
		for (int i = 0; i < 64; i++)
			r4300i_CoP1_D[i] = R4300i_UnknownOpcode;
		r4300i_CoP1_D[0] = r4300i_COP1_D_ADD;
		r4300i_CoP1_D[1] = r4300i_COP1_D_SUB;
		r4300i_CoP1_D[2] = r4300i_COP1_D_MUL;
		r4300i_CoP1_D[3] = r4300i_COP1_D_DIV;
		r4300i_CoP1_D[4] = r4300i_COP1_D_SQRT;
		r4300i_CoP1_D[5] = r4300i_COP1_D_ABS;
		r4300i_CoP1_D[6] = r4300i_COP1_D_MOV;
		r4300i_CoP1_D[7] = r4300i_COP1_D_NEG;
		r4300i_CoP1_D[12] = r4300i_COP1_D_ROUND_W;
		r4300i_CoP1_D[13] = r4300i_COP1_D_TRUNC_W;
		r4300i_CoP1_D[32] = r4300i_COP1_D_CVT_S;
		r4300i_CoP1_D[36] = r4300i_COP1_D_CVT_W;
		r4300i_CoP1_D[37] = r4300i_COP1_D_CVT_L;
		for (int i = 48; i < 64; i++)
			r4300i_CoP1_D[i] = r4300i_COP1_D_CMP;

		r4300i_CoP1_W = new OpCode[64];
		for (int i = 0; i < 64; i++)
			r4300i_CoP1_W[i] = R4300i_UnknownOpcode;
		r4300i_CoP1_W[32] = r4300i_COP1_W_CVT_S;
		r4300i_CoP1_W[33] = r4300i_COP1_W_CVT_D;

		r4300i_CoP1_L = new OpCode[64];
		for (int i = 0; i < 64; i++)
			r4300i_CoP1_L[i] = R4300i_UnknownOpcode;
		r4300i_CoP1_L[32] = r4300i_COP1_L_CVT_S;
		r4300i_CoP1_L[33] = r4300i_COP1_L_CVT_D;
	}

	/************************* OpCode functions *************************/

	protected OpCode R4300i_opcode_COP1_BC = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			r4300i_CoP1_BC[(inst >> FT) & 0x1F].exec(inst, unused);
		}
	};

	protected OpCode R4300i_opcode_COP1_S = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			r4300i_CoP1_S[inst & 0x3F].exec(inst, unused);
		}
	};

	protected OpCode R4300i_opcode_COP1_D = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			r4300i_CoP1_D[inst & 0x3F].exec(inst, unused);
		}
	};

	protected OpCode R4300i_opcode_COP1_W = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			r4300i_CoP1_W[inst & 0x3F].exec(inst, unused);
		}
	};

	protected OpCode R4300i_opcode_COP1_L = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			r4300i_CoP1_L[inst & 0x3F].exec(inst, unused);
		}
	};

	/************************** COP1 functions **************************/

	protected OpCode r4300i_COP1_MF = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			bus0.write32bit((inst >> FT) & 0x1F, FPR[(inst >> FS) & 0x1F].getW((inst >> FS) & 0x1F));
		}
	};

	protected OpCode r4300i_COP1_DMF = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			bus0DW.write64bit((inst >> FT) & 0x1F, FPR[(inst >> FS) & 0x1F].DW);
		}
	};

	protected OpCode r4300i_COP1_CF = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			try
			{
				if (((inst >> FS) & 0x1F) != 31 && ((inst >> FS) & 0x1F) != 0)
				{
					System.err.printf("CFC1 what register are you reading from ?\n");
					bus0.write32bit((inst >> FT) & 0x1F, 0);
					return;
				}
				bus0.write32bit((inst >> FT) & 0x1F, FPCR[(inst >> FS) & 0x1F]);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	};

	protected OpCode r4300i_COP1_MT = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FS) & 0x1F].setW((inst >> FS) & 0x1F, bus0.read32bit((inst >> FT) & 0x1F));
		}
	};

	protected OpCode r4300i_COP1_DMT = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FS) & 0x1F].DW = bus0DW.read64bit((inst >> FT) & 0x1F);
		}
	};

	protected OpCode r4300i_COP1_CT = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			try
			{
				if (((inst >> FS) & 0x1F) != 31)
				{
					System.err.printf("CTC1 what register are you writing to ?\n");
					return;
				}
				FPCR[31] = bus0.read32bit((inst >> FT) & 0x1F);
				switch (FPCR[31] & 3)
				{
				case 0:
					roundingModel = RC_NEAR;
					break;
				case 1:
					roundingModel = RC_CHOP;
					break;
				case 2:
					roundingModel = RC_UP;
					break;
				case 3:
					roundingModel = RC_DOWN;
					break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	};

	/************************* COP1: BC1 functions ***********************/

	protected OpCode r4300i_COP1_BCF = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			if ((FPCR[FSTATUS_REGISTER] & FPCSR_C) == 0)
			{
				bus0.write32bit(PC_REGISTER, bus0.read32bit(PC_REGISTER) + (((short) inst) << 2));
				bus0.write32bit(DELAY_SLOT_REGISTER, 1);
			}
		}
	};

	protected OpCode r4300i_COP1_BCT = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			if ((FPCR[FSTATUS_REGISTER] & FPCSR_C) != 0)
			{
				bus0.write32bit(PC_REGISTER, bus0.read32bit(PC_REGISTER) + (((short) inst) << 2));
				bus0.write32bit(DELAY_SLOT_REGISTER, 1);
			}
		}
	};

	protected OpCode r4300i_COP1_BCFL = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			if ((FPCR[FSTATUS_REGISTER] & FPCSR_C) == 0)
			{
				bus0.write32bit(PC_REGISTER, bus0.read32bit(PC_REGISTER) + (((short) inst) << 2));
				bus0.write32bit(DELAY_SLOT_REGISTER, 1);
			}
			else
			{
				bus0.write32bit(DELAY_SLOT_REGISTER, 0);
			}
		}
	};

	protected OpCode r4300i_COP1_BCTL = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			if ((FPCR[FSTATUS_REGISTER] & FPCSR_C) != 0)
			{
				bus0.write32bit(PC_REGISTER, bus0.read32bit(PC_REGISTER) + (((short) inst) << 2));
				bus0.write32bit(DELAY_SLOT_REGISTER, 1);
			}
			else
			{
				bus0.write32bit(DELAY_SLOT_REGISTER, 0);
			}
		}
	};

	/************************** COP1: S functions ************************/

	protected OpCode r4300i_COP1_S_ADD = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F) + FPR[(inst >> FT) & 0x1F].getF((inst >> FT) & 0x1F));
		}
	};

	protected OpCode r4300i_COP1_S_SUB = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F) - FPR[(inst >> FT) & 0x1F].getF((inst >> FT) & 0x1F));
		}
	};

	protected OpCode r4300i_COP1_S_MUL = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F) * FPR[(inst >> FT) & 0x1F].getF((inst >> FT) & 0x1F));
		}
	};

	protected OpCode r4300i_COP1_S_DIV = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F) / FPR[(inst >> FT) & 0x1F].getF((inst >> FT) & 0x1F));
		}
	};

	protected OpCode r4300i_COP1_S_SQRT = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, BigDecimal.valueOf(StrictMath.sqrt(FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F))).round(floatContext[roundingModel]).floatValue());
		}
	};

	protected OpCode r4300i_COP1_S_ABS = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, StrictMath.abs(FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F)));
		}
	};

	protected OpCode r4300i_COP1_S_MOV = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F));
		}
	};

	protected OpCode r4300i_COP1_S_NEG = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, BigDecimal.valueOf(FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F)).negate(floatContext[roundingModel]).floatValue());
		}
	};

	protected OpCode r4300i_COP1_S_TRUNC_L = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].DW = (long) (double) FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F);
		}
	};

	protected OpCode r4300i_COP1_S_ROUND_W = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setW((inst >> FD) & 0x1F, BigDecimal.valueOf(FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F)).setScale(0, roundingMode[RC_NEAR]).intValue());
		}
	};

	protected OpCode r4300i_COP1_S_TRUNC_W = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setW((inst >> FD) & 0x1F, (int) FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F));
		}
	};

	protected OpCode r4300i_COP1_S_FLOOR_W = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setW((inst >> FD) & 0x1F, (int) StrictMath.floor(FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F)));
		}
	};

	protected OpCode r4300i_COP1_S_CVT_D = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD((double) FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F));
		}
	};

	protected OpCode r4300i_COP1_S_CVT_W = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setW((inst >> FD) & 0x1F, BigDecimal.valueOf(FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F)).setScale(0, roundingMode[roundingModel]).intValue());
		}
	};

	protected OpCode r4300i_COP1_S_CVT_L = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].DW = BigDecimal.valueOf(FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F)).setScale(0, roundingMode[roundingModel]).longValue();
		}
	};

	protected OpCode r4300i_COP1_S_CMP = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			boolean less;
			boolean equal;
			boolean unorded;
			float temp0 = FPR[(inst >> FS) & 0x1F].getF((inst >> FS) & 0x1F);
			float temp1 = FPR[(inst >> FT) & 0x1F].getF((inst >> FT) & 0x1F);

			if (Float.isNaN(temp0) || Float.isNaN(temp1))
			{
				System.err.printf("Nan ?\n");
				less = false;
				equal = false;
				unorded = true;
				if (((inst & 0x3F) & 8) != 0)
				{
					System.err.printf("Signal InvalidOperationException\nin r4300i_COP1_S_CMP\n%X  %ff\n%X  %ff\n", temp0, temp0, temp1, temp1);
				}
			}
			else
			{
				less = temp0 < temp1;
				equal = temp0 == temp1;
				unorded = false;
			}

			boolean condition = ((((inst & 0x3F) & 4) == 0 ? false : true) && less)
					| ((((inst & 0x3F) & 2) == 0 ? false : true) && equal)
					| ((((inst & 0x3F) & 1) == 0 ? false : true) && unorded);

			if (condition)
			{
				FPCR[FSTATUS_REGISTER] |= FPCSR_C;
			}
			else
			{
				FPCR[FSTATUS_REGISTER] &= ~FPCSR_C;
			}
		}
	};

	/************************** COP1: D functions ************************/

	protected OpCode r4300i_COP1_D_ADD = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD(FPR[(inst >> FS) & 0x1F].getD() + FPR[(inst >> FT) & 0x1F].getD());
		}
	};

	protected OpCode r4300i_COP1_D_SUB = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD(FPR[(inst >> FS) & 0x1F].getD() - FPR[(inst >> FT) & 0x1F].getD());
		}
	};

	protected OpCode r4300i_COP1_D_MUL = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD(FPR[(inst >> FS) & 0x1F].getD() * FPR[(inst >> FT) & 0x1F].getD());
		}
	};

	protected OpCode r4300i_COP1_D_DIV = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD(FPR[(inst >> FS) & 0x1F].getD() / FPR[(inst >> FT) & 0x1F].getD());
		}
	};

	protected OpCode r4300i_COP1_D_SQRT = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD(StrictMath.sqrt(FPR[(inst >> FS) & 0x1F].getD()));
		}
	};

	protected OpCode r4300i_COP1_D_ABS = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD(StrictMath.abs(FPR[(inst >> FS) & 0x1F].getD()));
		}
	};

	protected OpCode r4300i_COP1_D_MOV = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD(FPR[(inst >> FS) & 0x1F].getD());
		}
	};

	protected OpCode r4300i_COP1_D_NEG = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD(FPR[(inst >> FS) & 0x1F].getD() * -1.0);
		}
	};

	protected OpCode r4300i_COP1_D_ROUND_W = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setW((inst >> FD) & 0x1F, BigDecimal.valueOf(FPR[(inst >> FS) & 0x1F].getD()).setScale(0, roundingMode[RC_NEAR]).intValue());
		}
	};

	protected OpCode r4300i_COP1_D_TRUNC_W = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setW((inst >> FD) & 0x1F, (int) FPR[(inst >> FS) & 0x1F].getD());
		}
	};

	protected OpCode r4300i_COP1_D_CVT_S = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, (float) FPR[(inst >> FS) & 0x1F].getD());
		}
	};

	protected OpCode r4300i_COP1_D_CVT_W = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setW((inst >> FD) & 0x1F, BigDecimal.valueOf(FPR[(inst >> FS) & 0x1F].getD()).setScale(0, roundingMode[roundingModel]).intValue());
		}
	};

	protected OpCode r4300i_COP1_D_CVT_L = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].DW = BigDecimal.valueOf(FPR[(inst >> FS) & 0x1F].getD()).setScale(0, roundingMode[roundingModel]).longValue();
		}
	};

	protected OpCode r4300i_COP1_D_CMP = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			boolean less;
			boolean equal;
			boolean unorded;
			double temp0 = FPR[(inst >> FS) & 0x1F].getD();
			double temp1 = FPR[(inst >> FT) & 0x1F].getD();

			if (Double.isNaN(temp0) || Double.isNaN(temp1))
			{
				System.err.printf("Nan ?\n");
				less = false;
				equal = false;
				unorded = true;
				if (((inst & 0x3F) & 8) != 0)
				{
					System.err.printf("Signal InvalidOperationException\nin r4300i_COP1_D_CMP\n");
				}
			}
			else
			{
				less = temp0 < temp1;
				equal = temp0 == temp1;
				unorded = false;
			}

			boolean condition = ((((inst & 0x3F) & 4) != 0) && less)
					| ((((inst & 0x3F) & 2) != 0) && equal)
					| ((((inst & 0x3F) & 1) != 0) && unorded);

			if (condition)
			{
				FPCR[FSTATUS_REGISTER] |= FPCSR_C;
			}
			else
			{
				FPCR[FSTATUS_REGISTER] &= ~FPCSR_C;
			}
		}
	};

	/************************** COP1: W functions ************************/

	protected OpCode r4300i_COP1_W_CVT_S = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, (float) FPR[(inst >> FS) & 0x1F].getW((inst >> FS) & 0x1F));
		}
	};

	protected OpCode r4300i_COP1_W_CVT_D = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD((double) FPR[(inst >> FS) & 0x1F].getW((inst >> FS) & 0x1F));
		}
	};

	/************************** COP1: L functions ************************/

	protected OpCode r4300i_COP1_L_CVT_S = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setF((inst >> FD) & 0x1F, BigDecimal.valueOf(FPR[(inst >> FS) & 0x1F].DW).round(floatContext[roundingModel]).floatValue());
		}
	};

	protected OpCode r4300i_COP1_L_CVT_D = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			FPR[(inst >> FD) & 0x1F].setD(BigDecimal.valueOf(FPR[(inst >> FS) & 0x1F].DW).round(floatContext[roundingModel]).doubleValue());
		}
	};

	/************************** Other functions **************************/

	protected OpCode R4300i_UnknownOpcode = new OpCode()
	{
		public void exec(int inst, int unused)
		{
			System.err.printf("PC:%X ,Unhandled r4300i FPU OpCode:%X\n", bus0.read32bit(PC_REGISTER), inst);
			System.exit(0);
		}
	};
}
