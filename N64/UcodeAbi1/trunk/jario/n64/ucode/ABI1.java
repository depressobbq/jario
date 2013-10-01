/**
 * Copyright 2006, 2013 Jason LaDere
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
 * Originally based on Azimer's Audio Plugin.
 *
 */

package jario.n64.ucode;

import jario.hardware.Bus16bit;
import jario.hardware.Bus64bit;
import jario.hardware.BusDMA;
import jario.hardware.Clockable;
import jario.hardware.Hardware;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/********
 * DMEM Memory Map for ABI 1 ***************
 * Address/Range Description
 * ------------- -------------------------------
 * 0x000..0x2BF UCodeData
 * 0x000-0x00F Constants - 0000 0001 0002 FFFF 0020 0800 7FFF 4000
 * 0x010-0x02F Function Jump Table (16 Functions * 2 bytes each = 32) 0x20
 * 0x030-0x03F Constants - F000 0F00 00F0 000F 0001 0010 0100 1000
 * 0x040-0x03F Used by the Envelope Mixer (But what for?)
 * 0x070-0x07F Used by the Envelope Mixer (But what for?)
 * 0x2C0..0x31F <Unknown>
 * 0x320..0x35F Segments
 * 0x360 Audio In Buffer (Location)
 * 0x362 Audio Out Buffer (Location)
 * 0x364 Audio Buffer Size (Location)
 * 0x366 Initial Volume for Left Channel
 * 0x368 Initial Volume for Right Channel
 * 0x36A Auxillary Buffer #1 (Location)
 * 0x36C Auxillary Buffer #2 (Location)
 * 0x36E Auxillary Buffer #3 (Location)
 * 0x370 Loop Value (shared location)
 * 0x370 Target Volume (Left)
 * 0x372 Ramp?? (Left)
 * 0x374 Rate?? (Left)
 * 0x376 Target Volume (Right)
 * 0x378 Ramp?? (Right)
 * 0x37A Rate?? (Right)
 * 0x37C Dry??
 * 0x37E Wet??
 * 0x380..0x4BF Alist data
 * 0x4C0..0x4FF ADPCM CodeBook
 * 0x500..0x5BF <Unknown>
 * 0x5C0..0xF7F Buffers...
 * 0xF80..0xFFF <Unknown>
 ***************************************************/
public class ABI1 implements Hardware, Clockable, Bus64bit
{
	public static interface OpCode
	{
		public void exec(int inst1, int inst2);
	}

	protected static final int A_INIT = 0x01;
	protected static final int A_CONTINUE = 0x00;
	protected static final int A_LOOP = 0x02;
	protected static final int A_OUT = 0x02;
	protected static final int A_LEFT = 0x02;
	protected static final int A_RIGHT = 0x00;
	protected static final int A_VOL = 0x04;
	protected static final int A_RATE = 0x00;
	protected static final int A_AUX = 0x08;
	protected static final int A_NOAUX = 0x00;
	protected static final int A_MAIN = 0x00;
	protected static final int A_MIX = 0x10;

	// T8 = 0x360
	protected static int AudioInBuffer; // 0x0000(T8)
	protected static int AudioOutBuffer; // 0x0002(T8)
	protected static int AudioCount; // 0x0004(T8)
	protected static short Vol_Left; // 0x0006(T8)
	protected static short Vol_Right; // 0x0008(T8)
	protected static int AudioAuxA; // 0x000A(T8)
	protected static int AudioAuxC; // 0x000C(T8)
	protected static int AudioAuxE; // 0x000E(T8)
	protected static int loopval; // 0x0010(T8) // Value set by A_SETLOOP : Possible conflict with SETVOLUME???
	protected static short VolTrg_Left; // 0x0010(T8)
	protected static int VolRamp_Left; // m_LeftVolTarget
	protected static short VolTrg_Right; // m_RightVol
	protected static int VolRamp_Right; // m_RightVolTarget
	protected static short Env_Dry; // 0x001C(T8)
	protected static short Env_Wet; // 0x001E(T8)

	protected static Bus16bit rdram;
	protected static BusDMA rdramDMA;
	protected static Hardware sp;

	protected OpCode[] alist = new OpCode[32];

	protected static ByteBuffer BufferSpace = ByteBuffer.allocate(0x10000); // 65536
	protected static ShortBuffer BufferSpaceShort = BufferSpace.asShortBuffer();
	protected static ByteBuffer hleMixerWorkArea = ByteBuffer.allocate(512);
	protected static short[] adpcmtable = new short[0x88];
	protected static short[] ResampleLUT =
	{
			(short) 0x0C39, (short) 0x66AD, (short) 0x0D46, (short) 0xFFDF, (short) 0x0B39, (short) 0x6696, (short) 0x0E5F, (short) 0xFFD8,
			(short) 0x0A44, (short) 0x6669, (short) 0x0F83, (short) 0xFFD0, (short) 0x095A, (short) 0x6626, (short) 0x10B4, (short) 0xFFC8,
			(short) 0x087D, (short) 0x65CD, (short) 0x11F0, (short) 0xFFBF, (short) 0x07AB, (short) 0x655E, (short) 0x1338, (short) 0xFFB6,
			(short) 0x06E4, (short) 0x64D9, (short) 0x148C, (short) 0xFFAC, (short) 0x0628, (short) 0x643F, (short) 0x15EB, (short) 0xFFA1,
			(short) 0x0577, (short) 0x638F, (short) 0x1756, (short) 0xFF96, (short) 0x04D1, (short) 0x62CB, (short) 0x18CB, (short) 0xFF8A,
			(short) 0x0435, (short) 0x61F3, (short) 0x1A4C, (short) 0xFF7E, (short) 0x03A4, (short) 0x6106, (short) 0x1BD7, (short) 0xFF71,
			(short) 0x031C, (short) 0x6007, (short) 0x1D6C, (short) 0xFF64, (short) 0x029F, (short) 0x5EF5, (short) 0x1F0B, (short) 0xFF56,
			(short) 0x022A, (short) 0x5DD0, (short) 0x20B3, (short) 0xFF48, (short) 0x01BE, (short) 0x5C9A, (short) 0x2264, (short) 0xFF3A,
			(short) 0x015B, (short) 0x5B53, (short) 0x241E, (short) 0xFF2C, (short) 0x0101, (short) 0x59FC, (short) 0x25E0, (short) 0xFF1E,
			(short) 0x00AE, (short) 0x5896, (short) 0x27A9, (short) 0xFF10, (short) 0x0063, (short) 0x5720, (short) 0x297A, (short) 0xFF02,
			(short) 0x001F, (short) 0x559D, (short) 0x2B50, (short) 0xFEF4, (short) 0xFFE2, (short) 0x540D, (short) 0x2D2C, (short) 0xFEE8,
			(short) 0xFFAC, (short) 0x5270, (short) 0x2F0D, (short) 0xFEDB, (short) 0xFF7C, (short) 0x50C7, (short) 0x30F3, (short) 0xFED0,
			(short) 0xFF53, (short) 0x4F14, (short) 0x32DC, (short) 0xFEC6, (short) 0xFF2E, (short) 0x4D57, (short) 0x34C8, (short) 0xFEBD,
			(short) 0xFF0F, (short) 0x4B91, (short) 0x36B6, (short) 0xFEB6, (short) 0xFEF5, (short) 0x49C2, (short) 0x38A5, (short) 0xFEB0,
			(short) 0xFEDF, (short) 0x47ED, (short) 0x3A95, (short) 0xFEAC, (short) 0xFECE, (short) 0x4611, (short) 0x3C85, (short) 0xFEAB,
			(short) 0xFEC0, (short) 0x4430, (short) 0x3E74, (short) 0xFEAC, (short) 0xFEB6, (short) 0x424A, (short) 0x4060, (short) 0xFEAF,
			(short) 0xFEAF, (short) 0x4060, (short) 0x424A, (short) 0xFEB6, (short) 0xFEAC, (short) 0x3E74, (short) 0x4430, (short) 0xFEC0,
			(short) 0xFEAB, (short) 0x3C85, (short) 0x4611, (short) 0xFECE, (short) 0xFEAC, (short) 0x3A95, (short) 0x47ED, (short) 0xFEDF,
			(short) 0xFEB0, (short) 0x38A5, (short) 0x49C2, (short) 0xFEF5, (short) 0xFEB6, (short) 0x36B6, (short) 0x4B91, (short) 0xFF0F,
			(short) 0xFEBD, (short) 0x34C8, (short) 0x4D57, (short) 0xFF2E, (short) 0xFEC6, (short) 0x32DC, (short) 0x4F14, (short) 0xFF53,
			(short) 0xFED0, (short) 0x30F3, (short) 0x50C7, (short) 0xFF7C, (short) 0xFEDB, (short) 0x2F0D, (short) 0x5270, (short) 0xFFAC,
			(short) 0xFEE8, (short) 0x2D2C, (short) 0x540D, (short) 0xFFE2, (short) 0xFEF4, (short) 0x2B50, (short) 0x559D, (short) 0x001F,
			(short) 0xFF02, (short) 0x297A, (short) 0x5720, (short) 0x0063, (short) 0xFF10, (short) 0x27A9, (short) 0x5896, (short) 0x00AE,
			(short) 0xFF1E, (short) 0x25E0, (short) 0x59FC, (short) 0x0101, (short) 0xFF2C, (short) 0x241E, (short) 0x5B53, (short) 0x015B,
			(short) 0xFF3A, (short) 0x2264, (short) 0x5C9A, (short) 0x01BE, (short) 0xFF48, (short) 0x20B3, (short) 0x5DD0, (short) 0x022A,
			(short) 0xFF56, (short) 0x1F0B, (short) 0x5EF5, (short) 0x029F, (short) 0xFF64, (short) 0x1D6C, (short) 0x6007, (short) 0x031C,
			(short) 0xFF71, (short) 0x1BD7, (short) 0x6106, (short) 0x03A4, (short) 0xFF7E, (short) 0x1A4C, (short) 0x61F3, (short) 0x0435,
			(short) 0xFF8A, (short) 0x18CB, (short) 0x62CB, (short) 0x04D1, (short) 0xFF96, (short) 0x1756, (short) 0x638F, (short) 0x0577,
			(short) 0xFFA1, (short) 0x15EB, (short) 0x643F, (short) 0x0628, (short) 0xFFAC, (short) 0x148C, (short) 0x64D9, (short) 0x06E4,
			(short) 0xFFB6, (short) 0x1338, (short) 0x655E, (short) 0x07AB, (short) 0xFFBF, (short) 0x11F0, (short) 0x65CD, (short) 0x087D,
			(short) 0xFFC8, (short) 0x10B4, (short) 0x6626, (short) 0x095A, (short) 0xFFD0, (short) 0x0F83, (short) 0x6669, (short) 0x0A44,
			(short) 0xFFD8, (short) 0x0E5F, (short) 0x6696, (short) 0x0B39, (short) 0xFFDF, (short) 0x0D46, (short) 0x66AD, (short) 0x0C39,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
	};

	public ABI1()
	{
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0: // not used
			break;
		case 1:
			rdram = (Bus16bit) bus;
			rdramDMA = (BusDMA) bus;
			break;
		case 2:
			sp = bus;
			break;
		}
	}

	@Override
	public void reset()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void clock(long ticks)
	{
		alist[0] = SPNOOP;
		alist[1] = ADPCM;
		alist[2] = CLEARBUFF;
		alist[3] = ENVMIXER;
		alist[4] = LOADBUFF;
		alist[5] = RESAMPLE;
		alist[6] = SAVEBUFF;
		alist[7] = UNKNOWN;
		alist[8] = SETBUFF;
		alist[9] = SETVOL;
		alist[10] = DMEMMOVE;
		alist[11] = LOADADPCM;
		alist[12] = MIXER;
		alist[13] = INTERLEAVE;
		alist[14] = UNKNOWN;
		alist[15] = SETLOOP;
		alist[16] = SPNOOP;
		alist[17] = SPNOOP;
		alist[18] = SPNOOP;
		alist[19] = SPNOOP;
		alist[20] = SPNOOP;
		alist[21] = SPNOOP;
		alist[22] = SPNOOP;
		alist[23] = SPNOOP;
		alist[24] = SPNOOP;
		alist[25] = SPNOOP;
		alist[26] = SPNOOP;
		alist[27] = SPNOOP;
		alist[28] = SPNOOP;
		alist[29] = SPNOOP;
		alist[30] = SPNOOP;
		alist[31] = SPNOOP;

		loopval = 0;
	}

	@Override
	public long read64bit(int pAddr)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void write64bit(int pAddr, long value)
	{
		int inst1 = (int) (value >> 32);
		int inst2 = (int) value;
		alist[inst1 >>> 24].exec(inst1, inst2);
	}

	/************************* OpCode functions *************************/

	protected OpCode SPNOOP = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			// System.out.println("SPNOOP");
		}
	};

	protected OpCode CLEARBUFF = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int addr = inst1 & 0xFFFC;
			int count = ((inst2 & 0xFFFF) + 3) & 0xFFFC;
			Arrays.fill(BufferSpace.array(), addr, addr + count, (byte) 0);
		}
	};

	protected OpCode ENVMIXER = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int flags = (inst1 >>> 16) & 0xFF;
			int addy = inst2 & 0xFFFFFF;
			ShortBuffer inp = BufferSpaceShort;
			int inp_p = AudioInBuffer / 2;
			ShortBuffer out = BufferSpaceShort;
			int out_p = AudioOutBuffer / 2;
			ShortBuffer aux1 = BufferSpaceShort;
			int aux1_p = AudioAuxA / 2;
			ShortBuffer aux2 = BufferSpaceShort;
			int aux2_p = AudioAuxC / 2;
			ShortBuffer aux3 = BufferSpaceShort;
			int aux3_p = AudioAuxE / 2;
			int MainR;
			int MainL;
			int AuxR;
			int AuxL;
			int i1, o1, a1, a2 = 0, a3 = 0;
			int AuxIncRate = 1;
			ShortBuffer zero = ShortBuffer.allocate(8);
			int LVol, RVol;
			int LAcc, RAcc;
			int LTrg, RTrg;
			short Wet, Dry;
			int ptr = 0;
			int RRamp, LRamp;
			int LAdderStart, RAdderStart, LAdderEnd, RAdderEnd;
			int oMainR, oMainL, oAuxR, oAuxL;

			if ((flags & A_INIT) != 0)
			{
				LVol = ((Vol_Left * (int) VolRamp_Left));
				RVol = ((Vol_Right * (int) VolRamp_Right));
				Wet = (short) Env_Wet;
				Dry = (short) Env_Dry;
				LTrg = (VolTrg_Left << 16);
				RTrg = (VolTrg_Right << 16);
				LAdderStart = Vol_Left << 16;
				RAdderStart = Vol_Right << 16;
				LAdderEnd = LVol;
				RAdderEnd = RVol;
				RRamp = VolRamp_Right;
				LRamp = VolRamp_Left;
			}
			else
			{
				rdramDMA.readDMA(addy, hleMixerWorkArea, 0, 80);
				Wet = hleMixerWorkArea.getShort(0 + 2); // 0-1
				Dry = hleMixerWorkArea.getShort(4 + 2); // 2-3
				LTrg = hleMixerWorkArea.getInt(8); // 4-5
				RTrg = hleMixerWorkArea.getInt(12); // 6-7
				LRamp = hleMixerWorkArea.getInt(16); // 8-9 (hleMixerWorkArea is a 16bit pointer)
				RRamp = hleMixerWorkArea.getInt(20); // 10-11
				LAdderEnd = hleMixerWorkArea.getInt(24); // 12-13
				RAdderEnd = hleMixerWorkArea.getInt(28); // 14-15
				LAdderStart = hleMixerWorkArea.getInt(32); // 12-13
				RAdderStart = hleMixerWorkArea.getInt(36); // 14-15
			}

			if ((flags & A_AUX) == 0)
			{
				AuxIncRate = 0;
				aux2 = aux3 = zero;
			}

			oMainL = (Dry * (LTrg >> 16) + 0x4000) >> 15;
			oAuxL = (Wet * (LTrg >> 16) + 0x4000) >> 15;
			oMainR = (Dry * (RTrg >> 16) + 0x4000) >> 15;
			oAuxR = (Wet * (RTrg >> 16) + 0x4000) >> 15;

			for (int y = 0; y < AudioCount; y += 16)
			{
				if (LAdderStart != LTrg)
				{
					LAcc = LAdderStart;
					LVol = (LAdderEnd - LAdderStart) >> 3;
					LAdderEnd = (int) (((long) LAdderEnd * (long) LRamp) >> 16);
					LAdderStart = (int) (((long) LAcc * (long) LRamp) >> 16);
				}
				else
				{
					LAcc = LTrg;
					LVol = 0;
				}

				if (RAdderStart != RTrg)
				{
					RAcc = RAdderStart;
					RVol = (RAdderEnd - RAdderStart) >> 3;
					RAdderEnd = (int) (((long) RAdderEnd * (long) RRamp) >> 16);
					RAdderStart = (int) (((long) RAcc * (long) RRamp) >> 16);
				}
				else
				{
					RAcc = RTrg;
					RVol = 0;
				}

				for (int x = 0; x < 8; x++)
				{
					i1 = (int) inp.get(inp_p + (ptr ^ 1));
					o1 = (int) out.get(out_p + (ptr ^ 1));
					a1 = (int) aux1.get(aux1_p + (ptr ^ 1));
					if (AuxIncRate != 0)
					{
						a2 = (int) aux2.get(aux2_p + (ptr ^ 1));
						a3 = (int) aux3.get(aux3_p + (ptr ^ 1));
					}
					// TODO: here...
					// LAcc = LTrg;
					// RAcc = RTrg;

					LAcc += LVol;
					RAcc += RVol;

					if (LVol <= 0)
					{ // Decrementing
						if (LAcc < LTrg)
						{
							LAcc = LTrg;
							LAdderStart = LTrg;
							MainL = oMainL;
							AuxL = oAuxL;
						}
						else
						{
							MainL = (Dry * ((int) LAcc >> 16) + 0x4000) >> 15;
							AuxL = (Wet * ((int) LAcc >> 16) + 0x4000) >> 15;
						}
					}
					else
					{
						if (LAcc > LTrg)
						{
							LAcc = LTrg;
							LAdderStart = LTrg;
							MainL = oMainL;
							AuxL = oAuxL;
						}
						else
						{
							MainL = (Dry * ((int) LAcc >> 16) + 0x4000) >> 15;
							AuxL = (Wet * ((int) LAcc >> 16) + 0x4000) >> 15;
						}
					}

					if (RVol <= 0)
					{ // Decrementing
						if (RAcc < RTrg)
						{
							RAcc = RTrg;
							RAdderStart = RTrg;
							MainR = oMainR;
							AuxR = oAuxR;
						}
						else
						{
							MainR = (Dry * ((int) RAcc >> 16) + 0x4000) >> 15;
							AuxR = (Wet * ((int) RAcc >> 16) + 0x4000) >> 15;
						}
					}
					else
					{
						if (RAcc > RTrg)
						{
							RAcc = RTrg;
							RAdderStart = RTrg;
							MainR = oMainR;
							AuxR = oAuxR;
						}
						else
						{
							MainR = (Dry * ((int) RAcc >> 16) + 0x4000) >> 15;
							AuxR = (Wet * ((int) RAcc >> 16) + 0x4000) >> 15;
						}
					}

					o1 += ((i1 * MainR) + 0x4000) >> 15;
					a1 += ((i1 * MainL) + 0x4000) >> 15;

					if (o1 > 32767)
					{
						o1 = 32767;
					}
					else if (o1 < -32768)
					{
						o1 = -32768;
					}

					if (a1 > 32767)
					{
						a1 = 32767;
					}
					else if (a1 < -32768)
					{
						a1 = -32768;
					}

					out.put(out_p + (ptr ^ 1), (short) o1);
					aux1.put(aux1_p + (ptr ^ 1), (short) a1);
					if (AuxIncRate != 0)
					{
						a2 += ((i1 * AuxR) + 0x4000) >> 15;
						a3 += ((i1 * AuxL) + 0x4000) >> 15;

						if (a2 > 32767)
						{
							a2 = 32767;
						}
						else if (a2 < -32768)
						{
							a2 = -32768;
						}

						if (a3 > 32767)
						{
							a3 = 32767;
						}
						else if (a3 < -32768)
						{
							a3 = -32768;
						}

						aux2.put(aux2_p + (ptr ^ 1), (short) a2);
						aux3.put(aux3_p + (ptr ^ 1), (short) a3);
					}
					ptr++;
				}
			}

			hleMixerWorkArea.putShort(0 + 2, Wet); // 0-1
			hleMixerWorkArea.putShort(4 + 2, Dry); // 2-3
			hleMixerWorkArea.putInt(8, LTrg); // 4-5
			hleMixerWorkArea.putInt(12, RTrg); // 6-7
			hleMixerWorkArea.putInt(16, LRamp); // 8-9 (hleMixerWorkArea is a 16bit pointer)
			hleMixerWorkArea.putInt(20, RRamp); // 10-11
			hleMixerWorkArea.putInt(24, LAdderEnd); // 12-13
			hleMixerWorkArea.putInt(28, RAdderEnd); // 14-15
			hleMixerWorkArea.putInt(32, LAdderStart); // 12-13
			hleMixerWorkArea.putInt(36, RAdderStart); // 14-15
			rdramDMA.writeDMA(addy, hleMixerWorkArea, 0, 80);
		}
	};

	protected OpCode RESAMPLE = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int flags = (inst1 >>> 16) & 0xFF;
			int Pitch = (inst1 & 0xFFFF) << 1;
			int addy = inst2 & 0xFFFFFF;
			int Accum = 0;
			int location;
			ShortBuffer dst = BufferSpaceShort;
			ShortBuffer src = BufferSpaceShort;
			int srcPtr = AudioInBuffer / 2;
			int dstPtr = AudioOutBuffer / 2;
			int temp;
			int accum;

			srcPtr -= 4;

			if ((flags & 0x1) == 0)
			{
				for (int x = 0; x < 4; x++)
				{
					src.put((srcPtr + x) ^ 1, rdram.read16bit((((addy / 2) + x) ^ 1) * 2));
				}
				Accum = rdram.read16bit(addy + 8) & 0xFFFF;
			}
			else
			{
				for (int x = 0; x < 4; x++)
				{
					src.put((srcPtr + x) ^ 1, (short) 0);
				}
			}

			int count = ((AudioCount + 0xF) & 0xFFF0) / 2;
			for (int i = 0; i < count; i++)
			{
				location = ((Accum >>> 10) * 8) / 2;

				temp = ((int) src.get((srcPtr + 0) ^ 1) * ((int) ((short) ResampleLUT[location + 0])));
				accum = (temp >> 15);

				temp = ((int) src.get((srcPtr + 1) ^ 1) * ((int) ((short) ResampleLUT[location + 1])));
				accum += (temp >> 15);

				temp = ((int) src.get((srcPtr + 2) ^ 1) * ((int) ((short) ResampleLUT[location + 2])));
				accum += (temp >> 15);

				temp = ((int) src.get((srcPtr + 3) ^ 1) * ((int) ((short) ResampleLUT[location + 3])));
				accum += (temp >> 15);

				if (accum > 32767)
				{
					accum = 32767;
				}
				if (accum < -32768)
				{
					accum = -32768;
				}

				dst.put(dstPtr ^ 1, (short) accum);
				dstPtr++;
				Accum += Pitch;
				srcPtr += (Accum >>> 16);
				Accum &= 0xFFFF;
			}

			for (int x = 0; x < 4; x++)
			{
				rdram.write16bit((((addy / 2) + x) ^ 1) * 2, src.get((srcPtr + x) ^ 1));
			}
			rdram.write16bit(addy + 8, (short) Accum);
		}
	};

	protected OpCode SETVOL = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int flags = (inst1 >>> 16) & 0xFF;
			int vol = inst1 & 0xFFFF;
			int volrate = inst2 & 0xFFFF;

			if ((flags & A_AUX) != 0)
			{
				Env_Dry = (short) vol; // m_MainVol
				Env_Wet = (short) volrate; // m_AuxVol
				return;
			}

			if ((flags & A_VOL) != 0)
			{ // Set the Source(start) Volumes
				if ((flags & A_LEFT) != 0)
				{
					Vol_Left = (short) vol; // m_LeftVolume
				}
				else
				{ // A_RIGHT
					Vol_Right = (short) vol; // m_RightVolume
				}
				return;
			}

			if ((flags & A_LEFT) != 0)
			{ // Set the Ramping values Target, Ramp
				VolTrg_Left = (short) inst1; // m_LeftVol
				VolRamp_Left = (int) inst2;
			}
			else
			{ // A_RIGHT
				VolTrg_Right = (short) inst1; // m_RightVol
				VolRamp_Right = (int) inst2;
			}
		}
	};

	protected OpCode UNKNOWN = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			// System.out.println("UNKNOWN");
		}
	};

	protected OpCode SETLOOP = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			loopval = inst2 & 0xFFFFFF;
		}
	};

	protected OpCode ADPCM = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int flags = (inst1 >>> 16) & 0xFF;
			int Address = inst2 & 0xFFFFFF;
			int inPtr = 0;
			ByteBuffer out = BufferSpace;
			int out_p = AudioOutBuffer;
			short count = (short) AudioCount;
			int icode;
			int code;
			int vscale;
			int index;
			int j;
			int[] a = new int[8];
			int book1_p, book2_p;

			Arrays.fill(out.array(), out_p, out_p + 32, (byte) 0);

			if ((flags & 0x1) == 0)
			{
				if ((flags & 0x2) != 0)
				{
					rdramDMA.readDMA(loopval & 0x7fffff, out, out_p, 32);
				}
				else
				{
					rdramDMA.readDMA(Address, out, out_p, 32);
				}
			}

			int l1 = out.getShort(out_p + 28);
			int l2 = out.getShort(out_p + 30);
			int[] inp1 = new int[8];
			int[] inp2 = new int[8];
			out_p += 32;
			while (count > 0)
			{
				code = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
				index = code & 0xF;
				index <<= 4; // index into the adpcm code table
				book1_p = index;
				book2_p = book1_p + 8;
				code >>= 4; // upper nibble is scale
				// very strange. 0x8000 would be .5 in 16:16 format
				// so this appears to be a fractional scale based
				// on the 12 based inverse of the scale value. note
				// that this could be negative, in which case we do
				// not use the calculated vscale value... see the
				// if(code>12) check below
				vscale = (0x8000 >> ((12 - code) - 1));
				inPtr++; // coded adpcm data lies next
				j = 0;
				while (j < 8)
				{ // loop of 8, for 8 coded nibbles from 4 bytes which yields 8 short pcm values
					icode = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
					inPtr++;

					inp1[j] = (short) ((icode & 0xF0) << 8); // this will in effect be signed
					if (code < 12)
					{
						inp1[j] = ((int) ((int) inp1[j] * (int) vscale) >> 16);
					}
					j++;

					inp1[j] = (short) ((icode & 0xF) << 12);
					if (code < 12)
					{
						inp1[j] = ((int) ((int) inp1[j] * (int) vscale) >> 16);
					}
					j++;
				}
				j = 0;
				while (j < 8)
				{
					icode = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
					inPtr++;

					inp2[j] = (short) ((icode & 0xF0) << 8); // this will in effect be signed
					if (code < 12)
					{
						inp2[j] = ((int) ((int) inp2[j] * (int) vscale) >> 16);
					}
					j++;

					inp2[j] = (short) ((icode & 0xF) << 12);
					if (code < 12)
					{
						inp2[j] = ((int) ((int) inp2[j] * (int) vscale) >> 16);
					}
					j++;
				}

				a[0] = (int) adpcmtable[book1_p + 0] * (int) l1;
				a[0] += (int) adpcmtable[book2_p + 0] * (int) l2;
				a[0] += (int) inp1[0] * (int) 2048;

				a[1] = (int) adpcmtable[book1_p + 1] * (int) l1;
				a[1] += (int) adpcmtable[book2_p + 1] * (int) l2;
				a[1] += (int) adpcmtable[book2_p + 0] * inp1[0];
				a[1] += (int) inp1[1] * (int) 2048;

				a[2] = (int) adpcmtable[book1_p + 2] * (int) l1;
				a[2] += (int) adpcmtable[book2_p + 2] * (int) l2;
				a[2] += (int) adpcmtable[book2_p + 1] * inp1[0];
				a[2] += (int) adpcmtable[book2_p + 0] * inp1[1];
				a[2] += (int) inp1[2] * (int) 2048;

				a[3] = (int) adpcmtable[book1_p + 3] * (int) l1;
				a[3] += (int) adpcmtable[book2_p + 3] * (int) l2;
				a[3] += (int) adpcmtable[book2_p + 2] * inp1[0];
				a[3] += (int) adpcmtable[book2_p + 1] * inp1[1];
				a[3] += (int) adpcmtable[book2_p + 0] * inp1[2];
				a[3] += (int) inp1[3] * (int) 2048;

				a[4] = (int) adpcmtable[book1_p + 4] * (int) l1;
				a[4] += (int) adpcmtable[book2_p + 4] * (int) l2;
				a[4] += (int) adpcmtable[book2_p + 3] * inp1[0];
				a[4] += (int) adpcmtable[book2_p + 2] * inp1[1];
				a[4] += (int) adpcmtable[book2_p + 1] * inp1[2];
				a[4] += (int) adpcmtable[book2_p + 0] * inp1[3];
				a[4] += (int) inp1[4] * (int) 2048;

				a[5] = (int) adpcmtable[book1_p + 5] * (int) l1;
				a[5] += (int) adpcmtable[book2_p + 5] * (int) l2;
				a[5] += (int) adpcmtable[book2_p + 4] * inp1[0];
				a[5] += (int) adpcmtable[book2_p + 3] * inp1[1];
				a[5] += (int) adpcmtable[book2_p + 2] * inp1[2];
				a[5] += (int) adpcmtable[book2_p + 1] * inp1[3];
				a[5] += (int) adpcmtable[book2_p + 0] * inp1[4];
				a[5] += (int) inp1[5] * (int) 2048;

				a[6] = (int) adpcmtable[book1_p + 6] * (int) l1;
				a[6] += (int) adpcmtable[book2_p + 6] * (int) l2;
				a[6] += (int) adpcmtable[book2_p + 5] * inp1[0];
				a[6] += (int) adpcmtable[book2_p + 4] * inp1[1];
				a[6] += (int) adpcmtable[book2_p + 3] * inp1[2];
				a[6] += (int) adpcmtable[book2_p + 2] * inp1[3];
				a[6] += (int) adpcmtable[book2_p + 1] * inp1[4];
				a[6] += (int) adpcmtable[book2_p + 0] * inp1[5];
				a[6] += (int) inp1[6] * (int) 2048;

				a[7] = (int) adpcmtable[book1_p + 7] * (int) l1;
				a[7] += (int) adpcmtable[book2_p + 7] * (int) l2;
				a[7] += (int) adpcmtable[book2_p + 6] * inp1[0];
				a[7] += (int) adpcmtable[book2_p + 5] * inp1[1];
				a[7] += (int) adpcmtable[book2_p + 4] * inp1[2];
				a[7] += (int) adpcmtable[book2_p + 3] * inp1[3];
				a[7] += (int) adpcmtable[book2_p + 2] * inp1[4];
				a[7] += (int) adpcmtable[book2_p + 1] * inp1[5];
				a[7] += (int) adpcmtable[book2_p + 0] * inp1[6];
				a[7] += (int) inp1[7] * (int) 2048;

				for (j = 0; j < 8; j++)
				{
					a[j] >>= 11;
					if (a[j] > 32767)
					{
						a[j] = 32767;
					}
					else if (a[j] < -32768)
					{
						a[j] = -32768;
					}
					out.putShort(out_p, (short) a[j]);
					out_p += 2;
				}
				l1 = a[6];
				l2 = a[7];

				a[0] = (int) adpcmtable[book1_p + 0] * (int) l1;
				a[0] += (int) adpcmtable[book2_p + 0] * (int) l2;
				a[0] += (int) inp2[0] * (int) 2048;

				a[1] = (int) adpcmtable[book1_p + 1] * (int) l1;
				a[1] += (int) adpcmtable[book2_p + 1] * (int) l2;
				a[1] += (int) adpcmtable[book2_p + 0] * inp2[0];
				a[1] += (int) inp2[1] * (int) 2048;

				a[2] = (int) adpcmtable[book1_p + 2] * (int) l1;
				a[2] += (int) adpcmtable[book2_p + 2] * (int) l2;
				a[2] += (int) adpcmtable[book2_p + 1] * inp2[0];
				a[2] += (int) adpcmtable[book2_p + 0] * inp2[1];
				a[2] += (int) inp2[2] * (int) 2048;

				a[3] = (int) adpcmtable[book1_p + 3] * (int) l1;
				a[3] += (int) adpcmtable[book2_p + 3] * (int) l2;
				a[3] += (int) adpcmtable[book2_p + 2] * inp2[0];
				a[3] += (int) adpcmtable[book2_p + 1] * inp2[1];
				a[3] += (int) adpcmtable[book2_p + 0] * inp2[2];
				a[3] += (int) inp2[3] * (int) 2048;

				a[4] = (int) adpcmtable[book1_p + 4] * (int) l1;
				a[4] += (int) adpcmtable[book2_p + 4] * (int) l2;
				a[4] += (int) adpcmtable[book2_p + 3] * inp2[0];
				a[4] += (int) adpcmtable[book2_p + 2] * inp2[1];
				a[4] += (int) adpcmtable[book2_p + 1] * inp2[2];
				a[4] += (int) adpcmtable[book2_p + 0] * inp2[3];
				a[4] += (int) inp2[4] * (int) 2048;

				a[5] = (int) adpcmtable[book1_p + 5] * (int) l1;
				a[5] += (int) adpcmtable[book2_p + 5] * (int) l2;
				a[5] += (int) adpcmtable[book2_p + 4] * inp2[0];
				a[5] += (int) adpcmtable[book2_p + 3] * inp2[1];
				a[5] += (int) adpcmtable[book2_p + 2] * inp2[2];
				a[5] += (int) adpcmtable[book2_p + 1] * inp2[3];
				a[5] += (int) adpcmtable[book2_p + 0] * inp2[4];
				a[5] += (int) inp2[5] * (int) 2048;

				a[6] = (int) adpcmtable[book1_p + 6] * (int) l1;
				a[6] += (int) adpcmtable[book2_p + 6] * (int) l2;
				a[6] += (int) adpcmtable[book2_p + 5] * inp2[0];
				a[6] += (int) adpcmtable[book2_p + 4] * inp2[1];
				a[6] += (int) adpcmtable[book2_p + 3] * inp2[2];
				a[6] += (int) adpcmtable[book2_p + 2] * inp2[3];
				a[6] += (int) adpcmtable[book2_p + 1] * inp2[4];
				a[6] += (int) adpcmtable[book2_p + 0] * inp2[5];
				a[6] += (int) inp2[6] * (int) 2048;

				a[7] = (int) adpcmtable[book1_p + 7] * (int) l1;
				a[7] += (int) adpcmtable[book2_p + 7] * (int) l2;
				a[7] += (int) adpcmtable[book2_p + 6] * inp2[0];
				a[7] += (int) adpcmtable[book2_p + 5] * inp2[1];
				a[7] += (int) adpcmtable[book2_p + 4] * inp2[2];
				a[7] += (int) adpcmtable[book2_p + 3] * inp2[3];
				a[7] += (int) adpcmtable[book2_p + 2] * inp2[4];
				a[7] += (int) adpcmtable[book2_p + 1] * inp2[5];
				a[7] += (int) adpcmtable[book2_p + 0] * inp2[6];
				a[7] += (int) inp2[7] * (int) 2048;

				for (j = 0; j < 8; j++)
				{
					a[j] >>= 11;
					if (a[j] > 32767)
					{
						a[j] = 32767;
					}
					else if (a[j] < -32768)
					{
						a[j] = -32768;
					}
					out.putShort(out_p, (short) a[j]);
					out_p += 2;
				}
				l1 = a[6];
				l2 = a[7];

				count -= 32;
			}
			out_p -= 32;
			rdramDMA.writeDMA(Address, out, out_p, 32);
		}
	};

	protected OpCode LOADBUFF = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			if (AudioCount == 0) {
			return;
			}
			int cnt = (AudioCount + 3) & 0xFFFC;
			int v0 = inst2 & 0xFFFFFC;
			rdramDMA.readDMA(v0, BufferSpace, AudioInBuffer & 0xFFFC, cnt);
		}
	};

	protected OpCode SAVEBUFF = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			if (AudioCount == 0) {
			return;
			}
			int cnt = (AudioCount + 3) & 0xFFFC;
			int v0 = inst2 & 0xFFFFFF;
			rdramDMA.writeDMA(v0, BufferSpace, AudioOutBuffer & 0xFFFC, cnt);
		}
	};

	protected OpCode SEGMENT = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{

		}
	};

	protected OpCode SETBUFF = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			if (((inst1 >>> 16) & 0x8) != 0)
			{
				// A_AUX - Auxillary Sound Buffer Settings
				AudioAuxA = inst1 & 0xFFFF;
				AudioAuxC = inst2 >>> 16;
				AudioAuxE = inst2 & 0xFFFF;
			}
			else
			{
				// A_MAIN - Main Sound Buffer Settings
				AudioInBuffer = inst1 & 0xFFFF; // 0x00
				AudioOutBuffer = inst2 >>> 16; // 0x02
				AudioCount = inst2 & 0xFFFF; // 0x04
			}
		}
	};

	protected OpCode DMEMMOVE = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			if ((inst2 & 0xFFFF) == 0) {
			return;
			}
			int v0 = inst1 & 0xFFFF;
			int v1 = inst2 >>> 16;
			int count = (inst2 + 3) & 0xFFFC;
			System.arraycopy(BufferSpace.array(), v0, BufferSpace.array(), v1, count);
		}
	};

	protected OpCode LOADADPCM = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int v0 = inst2 & 0xFFFFFF;
			int cnt = (inst1 & 0xFFFF) >> 4;
			for (int x = 0; x < cnt; x++)
			{
				adpcmtable[0 + (x << 3)] = rdram.read16bit(v0 + 0);
				adpcmtable[1 + (x << 3)] = rdram.read16bit(v0 + 2);
				adpcmtable[2 + (x << 3)] = rdram.read16bit(v0 + 4);
				adpcmtable[3 + (x << 3)] = rdram.read16bit(v0 + 6);
				adpcmtable[4 + (x << 3)] = rdram.read16bit(v0 + 8);
				adpcmtable[5 + (x << 3)] = rdram.read16bit(v0 + 10);
				adpcmtable[6 + (x << 3)] = rdram.read16bit(v0 + 12);
				adpcmtable[7 + (x << 3)] = rdram.read16bit(v0 + 14);
				v0 += 16;
			}
		}
	};

	protected OpCode INTERLEAVE = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			ShortBuffer outbuff = BufferSpaceShort;
			int outbuff_p = AudioOutBuffer / 2;
			ShortBuffer inSrcR;
			ShortBuffer inSrcL;
			int inSrcR_p;
			int inSrcL_p;

			inSrcR = BufferSpaceShort;
			inSrcR_p = (inst2 >>> 16) / 2;
			inSrcL = BufferSpaceShort;
			inSrcL_p = (inst2 & 0xFFFF) / 2;

			for (int x = 0; x < (AudioCount / 4); x++)
			{
				outbuff.put(outbuff_p++, inSrcL.get(inSrcL_p++));
				outbuff.put(outbuff_p++, inSrcR.get(inSrcR_p++));
				outbuff.put(outbuff_p++, inSrcL.get(inSrcL_p++));
				outbuff.put(outbuff_p++, inSrcR.get(inSrcR_p++));
			}
		}
	};

	protected OpCode MIXER = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int dmemin = inst2 >>> 16;
			int dmemout = inst2 & 0xFFFF;
			int gain = (short) (inst1 & 0xFFFF);
			int temp;

			if (AudioCount == 0) {
			return;
			}

			for (int x = 0; x < AudioCount; x += 2)
			{
				temp = (BufferSpace.getShort(dmemin + x) * gain) >> 15;
				temp += BufferSpace.getShort(dmemout + x);

				if (temp > 32767)
				{
					temp = 32767;
				}
				if (temp < -32768)
				{
					temp = -32768;
				}

				BufferSpace.putShort(dmemout + x, (short) temp);
			}
		}
	};
}
