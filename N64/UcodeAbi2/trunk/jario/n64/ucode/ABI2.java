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

import jario.n64.ucode.ABI1;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class ABI2 extends ABI1
{
	private static boolean isMKABI = false;
	private static boolean isZeldaABI = false;
	private static int t3, s5, s6;
	private static short[] env = new short[8];

	@Override
	public void clock(long ticks)
	{
		alist[0] = SPNOOP;
		alist[1] = ADPCM2;
		alist[2] = CLEARBUFF2;
		alist[3] = UNKNOWN;
		alist[4] = ADDMIXER;
		alist[5] = RESAMPLE2;
		alist[6] = UNKNOWN;
		alist[7] = SEGMENT2;
		alist[8] = SETBUFF2;
		alist[9] = DUPLICATE2;
		alist[10] = DMEMMOVE2;
		alist[11] = LOADADPCM2;
		alist[12] = MIXER2;
		alist[13] = INTERLEAVE2;
		alist[14] = HILOGAIN;
		alist[15] = SETLOOP2;
		alist[16] = SPNOOP;
		alist[17] = INTERL2;
		alist[18] = ENVSETUP1;
		alist[19] = ENVMIXER2;
		alist[20] = LOADBUFF2;
		alist[21] = SAVEBUFF2;
		alist[22] = ENVSETUP2;
		alist[23] = SPNOOP;
		alist[24] = HILOGAIN;
		alist[25] = SPNOOP;
		alist[26] = DUPLICATE2;
		alist[27] = UNKNOWN;
		alist[28] = SPNOOP;
		alist[29] = SPNOOP;
		alist[30] = SPNOOP;
		alist[31] = SPNOOP;

		loopval = 0;
	}

	/************************* OpCode functions *************************/

	protected OpCode LOADADPCM2 = new OpCode()
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

	protected OpCode SETLOOP2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			loopval = inst2 & 0xFFFFFF;
		}
	};

	protected OpCode SETBUFF2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			AudioInBuffer = inst1 & 0xFFFF; // 0x00
			AudioOutBuffer = inst2 >>> 16; // 0x02
			AudioCount = inst2 & 0xFFFF; // 0x04
		}
	};

	protected OpCode ADPCM2 = new OpCode()
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
			int srange;
			int mask1;
			int mask2;
			int shifter;

			Arrays.fill(out.array(), out_p, out_p + 32, (byte) 0);

			// Tricky lil Zelda MM and ABI2!!! hahaha I know your secrets! :DDD
			if ((flags & 0x4) != 0)
			{
				srange = 0xE;
				mask1 = 0xC0;
				mask2 = 0x30;
				shifter = 10;
			}
			else
			{
				srange = 0xC;
				mask1 = 0xF0;
				mask2 = 0x0F;
				shifter = 12;
			}

			if ((flags & 0x1) == 0)
			{
				if ((flags & 0x2) != 0)
				{
					rdramDMA.readDMA(loopval, out, out_p, 32);
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
				index <<= 4;
				book1_p = index;
				book2_p = book1_p + 8;
				code >>= 4;
				vscale = (0x8000 >> ((srange - code) - 1));

				inPtr++;
				j = 0;

				while (j < 8)
				{
					icode = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
					inPtr++;

					inp1[j] = (short) ((icode & mask1) << 8); // this will in effect be signed
					if (code < srange)
					{
						inp1[j] = (inp1[j] * vscale) >> 16;
					}
					j++;

					inp1[j] = (short) ((icode & mask2) << shifter);
					if (code < srange)
					{
						inp1[j] = (inp1[j] * vscale) >> 16;
					}
					j++;

					if ((flags & 4) != 0)
					{
						inp1[j] = (short) ((icode & 0xC) << 12); // this will in effect be signed
						if (code < 0xE)
						{
							inp1[j] = (inp1[j] * vscale) >> 16;
						}
						j++;

						inp1[j] = (short) ((icode & 0x3) << 14);
						if (code < 0xE)
						{
							inp1[j] = (inp1[j] * vscale) >> 16;
						}
						j++;
					}
				}

				j = 0;
				while (j < 8)
				{
					icode = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
					inPtr++;

					inp2[j] = (short) ((icode & mask1) << 8);
					if (code < srange)
					{
						inp2[j] = (inp2[j] * vscale) >> 16;
					}
					j++;

					inp2[j] = (short) ((icode & mask2) << shifter);
					if (code < srange)
					{
						inp2[j] = (inp2[j] * vscale) >> 16;
					}
					j++;

					if ((flags & 4) != 0)
					{
						inp2[j] = (short) ((icode & 0xC) << 12);
						if (code < 0xE)
						{
							inp2[j] = (inp2[j] * vscale) >> 16;
						}
						j++;

						inp2[j] = (short) ((icode & 0x3) << 14);
						if (code < 0xE)
						{
							inp2[j] = (inp2[j] * vscale) >> 16;
						}
						j++;
					}
				}

				a[0] = (int) adpcmtable[book1_p + 0] * l1;
				a[0] += (int) adpcmtable[book2_p + 0] * l2;
				a[0] += inp1[0] * 2048;

				a[1] = (int) adpcmtable[book1_p + 1] * l1;
				a[1] += (int) adpcmtable[book2_p + 1] * l2;
				a[1] += (int) adpcmtable[book2_p + 0] * inp1[0];
				a[1] += inp1[1] * 2048;

				a[2] = (int) adpcmtable[book1_p + 2] * l1;
				a[2] += (int) adpcmtable[book2_p + 2] * l2;
				a[2] += (int) adpcmtable[book2_p + 1] * inp1[0];
				a[2] += (int) adpcmtable[book2_p + 0] * inp1[1];
				a[2] += inp1[2] * 2048;

				a[3] = (int) adpcmtable[book1_p + 3] * l1;
				a[3] += (int) adpcmtable[book2_p + 3] * l2;
				a[3] += (int) adpcmtable[book2_p + 2] * inp1[0];
				a[3] += (int) adpcmtable[book2_p + 1] * inp1[1];
				a[3] += (int) adpcmtable[book2_p + 0] * inp1[2];
				a[3] += inp1[3] * 2048;

				a[4] = (int) adpcmtable[book1_p + 4] * l1;
				a[4] += (int) adpcmtable[book2_p + 4] * l2;
				a[4] += (int) adpcmtable[book2_p + 3] * inp1[0];
				a[4] += (int) adpcmtable[book2_p + 2] * inp1[1];
				a[4] += (int) adpcmtable[book2_p + 1] * inp1[2];
				a[4] += (int) adpcmtable[book2_p + 0] * inp1[3];
				a[4] += inp1[4] * 2048;

				a[5] = (int) adpcmtable[book1_p + 5] * l1;
				a[5] += (int) adpcmtable[book2_p + 5] * l2;
				a[5] += (int) adpcmtable[book2_p + 4] * inp1[0];
				a[5] += (int) adpcmtable[book2_p + 3] * inp1[1];
				a[5] += (int) adpcmtable[book2_p + 2] * inp1[2];
				a[5] += (int) adpcmtable[book2_p + 1] * inp1[3];
				a[5] += (int) adpcmtable[book2_p + 0] * inp1[4];
				a[5] += inp1[5] * 2048;

				a[6] = (int) adpcmtable[book1_p + 6] * l1;
				a[6] += (int) adpcmtable[book2_p + 6] * l2;
				a[6] += (int) adpcmtable[book2_p + 5] * inp1[0];
				a[6] += (int) adpcmtable[book2_p + 4] * inp1[1];
				a[6] += (int) adpcmtable[book2_p + 3] * inp1[2];
				a[6] += (int) adpcmtable[book2_p + 2] * inp1[3];
				a[6] += (int) adpcmtable[book2_p + 1] * inp1[4];
				a[6] += (int) adpcmtable[book2_p + 0] * inp1[5];
				a[6] += inp1[6] * 2048;

				a[7] = (int) adpcmtable[book1_p + 7] * l1;
				a[7] += (int) adpcmtable[book2_p + 7] * l2;
				a[7] += (int) adpcmtable[book2_p + 6] * inp1[0];
				a[7] += (int) adpcmtable[book2_p + 5] * inp1[1];
				a[7] += (int) adpcmtable[book2_p + 4] * inp1[2];
				a[7] += (int) adpcmtable[book2_p + 3] * inp1[3];
				a[7] += (int) adpcmtable[book2_p + 2] * inp1[4];
				a[7] += (int) adpcmtable[book2_p + 1] * inp1[5];
				a[7] += (int) adpcmtable[book2_p + 0] * inp1[6];
				a[7] += inp1[7] * 2048;

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

				a[0] = (int) adpcmtable[book1_p + 0] * l1;
				a[0] += (int) adpcmtable[book2_p + 0] * l2;
				a[0] += inp2[0] * 2048;

				a[1] = (int) adpcmtable[book1_p + 1] * l1;
				a[1] += (int) adpcmtable[book2_p + 1] * l2;
				a[1] += (int) adpcmtable[book2_p + 0] * inp2[0];
				a[1] += inp2[1] * 2048;

				a[2] = (int) adpcmtable[book1_p + 2] * l1;
				a[2] += (int) adpcmtable[book2_p + 2] * l2;
				a[2] += (int) adpcmtable[book2_p + 1] * inp2[0];
				a[2] += (int) adpcmtable[book2_p + 0] * inp2[1];
				a[2] += inp2[2] * 2048;

				a[3] = (int) adpcmtable[book1_p + 3] * l1;
				a[3] += (int) adpcmtable[book2_p + 3] * l2;
				a[3] += (int) adpcmtable[book2_p + 2] * inp2[0];
				a[3] += (int) adpcmtable[book2_p + 1] * inp2[1];
				a[3] += (int) adpcmtable[book2_p + 0] * inp2[2];
				a[3] += inp2[3] * 2048;

				a[4] = (int) adpcmtable[book1_p + 4] * l1;
				a[4] += (int) adpcmtable[book2_p + 4] * l2;
				a[4] += (int) adpcmtable[book2_p + 3] * inp2[0];
				a[4] += (int) adpcmtable[book2_p + 2] * inp2[1];
				a[4] += (int) adpcmtable[book2_p + 1] * inp2[2];
				a[4] += (int) adpcmtable[book2_p + 0] * inp2[3];
				a[4] += inp2[4] * 2048;

				a[5] = (int) adpcmtable[book1_p + 5] * l1;
				a[5] += (int) adpcmtable[book2_p + 5] * l2;
				a[5] += (int) adpcmtable[book2_p + 4] * inp2[0];
				a[5] += (int) adpcmtable[book2_p + 3] * inp2[1];
				a[5] += (int) adpcmtable[book2_p + 2] * inp2[2];
				a[5] += (int) adpcmtable[book2_p + 1] * inp2[3];
				a[5] += (int) adpcmtable[book2_p + 0] * inp2[4];
				a[5] += inp2[5] * 2048;

				a[6] = (int) adpcmtable[book1_p + 6] * l1;
				a[6] += (int) adpcmtable[book2_p + 6] * l2;
				a[6] += (int) adpcmtable[book2_p + 5] * inp2[0];
				a[6] += (int) adpcmtable[book2_p + 4] * inp2[1];
				a[6] += (int) adpcmtable[book2_p + 3] * inp2[2];
				a[6] += (int) adpcmtable[book2_p + 2] * inp2[3];
				a[6] += (int) adpcmtable[book2_p + 1] * inp2[4];
				a[6] += (int) adpcmtable[book2_p + 0] * inp2[5];
				a[6] += inp2[6] * 2048;

				a[7] = (int) adpcmtable[book1_p + 7] * l1;
				a[7] += (int) adpcmtable[book2_p + 7] * l2;
				a[7] += (int) adpcmtable[book2_p + 6] * inp2[0];
				a[7] += (int) adpcmtable[book2_p + 5] * inp2[1];
				a[7] += (int) adpcmtable[book2_p + 4] * inp2[2];
				a[7] += (int) adpcmtable[book2_p + 3] * inp2[3];
				a[7] += (int) adpcmtable[book2_p + 2] * inp2[4];
				a[7] += (int) adpcmtable[book2_p + 1] * inp2[5];
				a[7] += (int) adpcmtable[book2_p + 0] * inp2[6];
				a[7] += inp2[7] * 2048;

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

	protected OpCode CLEARBUFF2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int addr = inst1 & 0xFFFF;
			int count = inst2 & 0xFFFF;
			if (count > 0)
			{
				Arrays.fill(BufferSpace.array(), addr, addr + count, (byte) 0);
			}
		}
	};

	protected OpCode LOADBUFF2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int cnt = ((inst1 >>> 0xC) + 3) & 0xFFC;
			int v0 = inst2 & 0xFFFFFC;
			rdramDMA.readDMA(v0, BufferSpace, inst1 & 0xFFFC, (cnt + 3) & 0xFFFC);
		}
	};

	protected OpCode SAVEBUFF2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int cnt = ((inst1 >>> 0xC) + 3) & 0xFFC;
			int v0 = inst2 & 0xFFFFFC;
			rdramDMA.writeDMA(v0, BufferSpace, inst1 & 0xFFFC, (cnt + 3) & 0xFFFC);
		}
	};

	protected OpCode MIXER2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int dmemin = inst2 >>> 16;
			int dmemout = inst2 & 0xFFFF;
			int count = (inst1 >>> 12) & 0xFF0;
			int gain = (short) (inst1 & 0xFFFF) * 2;
			int temp; // signed

			for (int x = 0; x < count; x += 2)
			{
				temp = (BufferSpace.getShort(dmemin + x) * gain) >> 16;
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

	protected OpCode RESAMPLE2 = new OpCode()
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
				location = (((Accum * 0x40) >>> 16) * 8) / 2;

				temp = (int) src.get((srcPtr + 0) ^ 1) * ((int) ResampleLUT[location + 0]);
				accum = (temp >> 15);

				temp = (int) src.get((srcPtr + 1) ^ 1) * ((int) ResampleLUT[location + 1]);
				accum += (temp >> 15);

				temp = (int) src.get((srcPtr + 2) ^ 1) * ((int) ResampleLUT[location + 2]);
				accum += (temp >> 15);

				temp = (int) src.get((srcPtr + 3) ^ 1) * ((int) ResampleLUT[location + 3]);
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

	protected OpCode DMEMMOVE2 = new OpCode()
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

	protected OpCode ENVSETUP1 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			t3 = inst1 & 0xFFFF;
			int tmp = (inst1 >>> 0x8) & 0xFF00;
			env[4] = (short) tmp;
			tmp += t3;
			env[5] = (short) tmp;
			s5 = inst2 >>> 16;
			s6 = inst2 & 0xFFFF;
		}
	};

	protected OpCode ENVSETUP2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int tmp = inst2 >>> 16;
			env[0] = (short) tmp;
			tmp += s5;
			env[1] = (short) tmp;
			tmp = inst2 & 0xFFFF;
			env[2] = (short) tmp;
			tmp += s6;
			env[3] = (short) tmp;
		}
	};

	protected OpCode ENVMIXER2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			ShortBuffer bufft6, bufft7, buffs0, buffs1, buffs3;
			int count;
			int adder;
			short vec9, vec10;
			short[] v2 = new short[8];
			buffs3 = BufferSpaceShort;
			int buffs3_p = ((inst1 >>> 0x0C) & 0x0FF0) / 2;
			bufft6 = BufferSpaceShort;
			int bufft6_p = ((inst2 >>> 0x14) & 0x0FF0) / 2;
			bufft7 = BufferSpaceShort;
			int bufft7_p = ((inst2 >>> 0x0C) & 0x0FF0) / 2;
			buffs0 = BufferSpaceShort;
			int buffs0_p = ((inst2 >>> 0x04) & 0x0FF0) / 2;
			buffs1 = BufferSpaceShort;
			int buffs1_p = ((inst2 << 0x04) & 0x0FF0) / 2;

			v2[0] = (short) (0 - (short) ((inst1 & 0x2) >>> 1));
			v2[1] = (short) (0 - (short) ((inst1 & 0x1)));
			v2[2] = (short) (0 - (short) ((inst1 & 0x8) >>> 1));
			v2[3] = (short) (0 - (short) ((inst1 & 0x4) >>> 1));

			count = (inst1 >>> 8) & 0xFF;

			if (!isMKABI)
			{
				s5 *= 2;
				s6 *= 2;
				t3 *= 2;
				adder = 0x10;
			}
			else
			{
				inst1 = 0;
				adder = 0x8;
				t3 = 0;
			}

			while (count > 0)
			{
				int temp;
				for (int x = 0; x < 0x8; x++)
				{
					vec9 = (short) ((short) (((int) buffs3.get(buffs3_p + (x ^ 1)) * (env[0] & 0xFFFF)) >> 16) ^ v2[0]);
					vec10 = (short) ((short) (((int) buffs3.get(buffs3_p + (x ^ 1)) * (env[2] & 0xFFFF)) >> 16) ^ v2[1]);
					temp = bufft6.get(bufft6_p + (x ^ 1)) + vec9;
					if (temp > 32767)
					{
						temp = 32767;
					}
					if (temp < -32768)
					{
						temp = -32768;
					}
					bufft6.put(bufft6_p + (x ^ 1), (short) temp);
					temp = bufft7.get(bufft7_p + (x ^ 1)) + vec10;
					if (temp > 32767)
					{
						temp = 32767;
					}
					if (temp < -32768)
					{
						temp = -32768;
					}
					bufft7.put(bufft7_p + (x ^ 1), (short) temp);
					vec9 = (short) ((short) (((int) vec9 * (env[4] & 0xFFFF)) >> 16) ^ v2[2]);
					vec10 = (short) ((short) (((int) vec10 * (env[4] & 0xFFFF)) >> 16) ^ v2[3]);
					if ((inst1 & 0x10) != 0)
					{
						temp = buffs0.get(buffs0_p + (x ^ 1)) + vec10;
						if (temp > 32767)
						{
							temp = 32767;
						}
						if (temp < -32768)
						{
							temp = -32768;
						}
						buffs0.put(buffs0_p + (x ^ 1), (short) temp);
						temp = buffs1.get(buffs1_p + (x ^ 1)) + vec9;
						if (temp > 32767)
						{
							temp = 32767;
						}
						if (temp < -32768)
						{
							temp = -32768;
						}
						buffs1.put(buffs1_p + (x ^ 1), (short) temp);
					}
					else
					{
						temp = buffs0.get(buffs0_p + (x ^ 1)) + vec9;
						if (temp > 32767)
						{
							temp = 32767;
						}
						if (temp < -32768)
						{
							temp = -32768;
						}
						buffs0.put(buffs0_p + (x ^ 1), (short) temp);
						temp = buffs1.get(buffs1_p + (x ^ 1)) + vec10;
						if (temp > 32767)
						{
							temp = 32767;
						}
						if (temp < -32768)
						{
							temp = -32768;
						}
						buffs1.put(buffs1_p + (x ^ 1), (short) temp);
					}
				}

				if (!isMKABI)
				{
					for (int x = 0x8; x < 0x10; x++)
					{
						vec9 = (short) ((short) (((int) buffs3.get(buffs3_p + (x ^ 1)) * (env[1] & 0xFFFF)) >> 16) ^ v2[0]);
						vec10 = (short) ((short) (((int) buffs3.get(buffs3_p + (x ^ 1)) * (env[3] & 0xFFFF)) >> 16) ^ v2[1]);
						temp = bufft6.get(bufft6_p + (x ^ 1)) + vec9;
						if (temp > 32767)
						{
							temp = 32767;
						}
						if (temp < -32768)
						{
							temp = -32768;
						}
						bufft6.put(bufft6_p + (x ^ 1), (short) temp);
						temp = bufft7.get(bufft7_p + (x ^ 1)) + vec10;
						if (temp > 32767)
						{
							temp = 32767;
						}
						if (temp < -32768)
						{
							temp = -32768;
						}
						bufft7.put(bufft7_p + (x ^ 1), (short) temp);
						vec9 = (short) ((short) (((int) vec9 * (env[5] & 0xFFFF)) >> 16) ^ v2[2]);
						vec10 = (short) ((short) (((int) vec10 * (env[5] & 0xFFFF)) >> 16) ^ v2[3]);
						if ((inst1 & 0x10) != 0)
						{
							temp = buffs0.get(buffs0_p + (x ^ 1)) + vec10;
							if (temp > 32767)
							{
								temp = 32767;
							}
							if (temp < -32768)
							{
								temp = -32768;
							}
							buffs0.put(buffs0_p + (x ^ 1), (short) temp);
							temp = buffs1.get(buffs1_p + (x ^ 1)) + vec9;
							if (temp > 32767)
							{
								temp = 32767;
							}
							if (temp < -32768)
							{
								temp = -32768;
							}
							buffs1.put(buffs1_p + (x ^ 1), (short) temp);
						}
						else
						{
							temp = buffs0.get(buffs0_p + (x ^ 1)) + vec9;
							if (temp > 32767)
							{
								temp = 32767;
							}
							if (temp < -32768)
							{
								temp = -32768;
							}
							buffs0.put(buffs0_p + (x ^ 1), (short) temp);
							temp = buffs1.get(buffs1_p + (x ^ 1)) + vec10;
							if (temp > 32767)
							{
								temp = 32767;
							}
							if (temp < -32768)
							{
								temp = -32768;
							}
							buffs1.put(buffs1_p + (x ^ 1), (short) temp);
						}
					}
				}
				bufft6_p += adder;
				bufft7_p += adder;
				buffs0_p += adder;
				buffs1_p += adder;
				buffs3_p += adder;
				count -= adder;

				env[0] += (s5 & 0xFFFF);
				env[1] += (s5 & 0xFFFF);
				env[2] += (s6 & 0xFFFF);
				env[3] += (s6 & 0xFFFF);
				env[4] += (t3 & 0xFFFF);
				env[5] += (t3 & 0xFFFF);
			}
		}
	};

	protected OpCode DUPLICATE2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int Count = (inst1 >>> 16) & 0xFF;
			int In = inst1 & 0xFFFF;
			int Out = inst2 >>> 16;
			byte[] buff = new byte[128];

			System.arraycopy(BufferSpace.array(), In, buff, 0, 128);

			while (Count != 0)
			{
				System.arraycopy(buff, 0, BufferSpace.array(), Out, 128);
				Out += 128;
				Count--;
			}
		}
	};

	protected OpCode INTERL2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int Count = inst1 & 0xFFFF;
			int Out = inst2 & 0xFFFF;
			int In = inst2 >>> 16;

			ByteBuffer src = BufferSpace; // [In];
			ByteBuffer dst = BufferSpace; // [Out];
			while (Count != 0)
			{
				dst.putShort(Out, src.getShort(In));
				Out += 2;
				In += 4;
				Count--;
			}
		}
	};

	protected OpCode INTERLEAVE2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			ShortBuffer outbuff;
			int outbuff_p;
			ShortBuffer inSrcR;
			ShortBuffer inSrcL;
			int inSrcR_p;
			int inSrcL_p;
			int count;

			count = (inst1 >>> 12) & 0xFF0;
			outbuff = BufferSpaceShort;
			if (count == 0)
			{
				outbuff_p = AudioOutBuffer / 2;
				count = AudioCount;
			}
			else
			{
				outbuff_p = (inst1 & 0xFFFF) / 2;
			}

			inSrcR = BufferSpaceShort;
			inSrcR_p = (inst2 & 0xFFFF) / 2;
			inSrcL = BufferSpaceShort;
			inSrcL_p = (inst2 >>> 16) / 2;

			for (int x = 0; x < (count / 4); x++)
			{
				outbuff.put(outbuff_p++, inSrcL.get(inSrcL_p++));
				outbuff.put(outbuff_p++, inSrcR.get(inSrcR_p++));
				outbuff.put(outbuff_p++, inSrcL.get(inSrcL_p++));
				outbuff.put(outbuff_p++, inSrcR.get(inSrcR_p++));
			}
		}
	};

	protected OpCode ADDMIXER = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			int Count = (inst1 >>> 12) & 0x00FF0;
			int InBuffer = inst2 >>> 16;
			int OutBuffer = inst2 & 0xFFFF;
			ShortBuffer inp, outp;
			int inp_p, outp_p;
			int temp;

			inp = BufferSpaceShort;
			inp_p = InBuffer / 2;
			outp = BufferSpaceShort;
			outp_p = OutBuffer / 2;

			for (int cntr = 0; cntr < Count; cntr += 2)
			{
				temp = outp.get(outp_p) + inp.get(inp_p);
				if (temp > 32767)
				{
					temp = 32767;
				}
				if (temp < -32768)
				{
					temp = -32768;
				}
				outp_p++;
				inp_p++;
			}
		}
	};

	protected OpCode HILOGAIN = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			// System.out.println("HILOGAIN");
			// u16 cnt = k0 & 0xffff;
			// u16 out = (t9 >> 16) & 0xffff;
			// s16 hi = (s16)((k0 >> 4) & 0xf000);
			// u16 lo = (k0 >> 20) & 0xf;
			// s16 *src;
			//
			// src = (s16 *)(BufferSpace+out);
			// s32 tmp, val;
			//
			// while(cnt) {
			// val = (s32)*src;
			// //tmp = ((val * (s32)hi) + ((u64)(val * lo) << 16) >> 16);
			// tmp = ((val * (s32)hi) >> 16) + (u32)(val * lo);
			// if ((s32)tmp > 32767) tmp = 32767;
			// else if ((s32)tmp < -32768) tmp = -32768;
			// *src = tmp;
			// src++;
			// cnt -= 2;
			// }
		}
	};

	// void FILTER2 () {
	// static int cnt = 0;
	// static s16 *lutt6;
	// static s16 *lutt5;
	// u8 *save = (rdram+(t9&0xFFFFFF));
	// u8 t4 = (u8)((k0 >> 0x10) & 0xFF);
	//
	// if (t4 > 1) { // Then set the cnt variable
	// cnt = (k0 & 0xFFFF);
	// lutt6 = (s16 *)save;
	// // memcpy (dmem+0xFE0, rdram+(t9&0xFFFFFF), 0x10);
	// return;
	// }
	//
	// if (t4 == 0) {
	// // memcpy (dmem+0xFB0, rdram+(t9&0xFFFFFF), 0x20);
	// lutt5 = (short *)(save+0x10);
	// }
	//
	// lutt5 = (short *)(save+0x10);
	//
	// // lutt5 = (short *)(dmem + 0xFC0);
	// // lutt6 = (short *)(dmem + 0xFE0);
	// for (int x = 0; x < 8; x++) {
	// s32 a;
	// a = (lutt5[x] + lutt6[x]) >> 1;
	// lutt5[x] = lutt6[x] = (short)a;
	// }
	// short *inp1, *inp2;
	// s32 out1[8];
	// s16 outbuff[0x3c0], *outp;
	// u32 inPtr = (u32)(k0&0xffff);
	// inp1 = (short *)(save);
	// outp = outbuff;
	// inp2 = (short *)(BufferSpace+inPtr);
	// for (x = 0; x < cnt; x+=0x10) {
	// out1[1] = inp1[0]*lutt6[6];
	// out1[1] += inp1[3]*lutt6[7];
	// out1[1] += inp1[2]*lutt6[4];
	// out1[1] += inp1[5]*lutt6[5];
	// out1[1] += inp1[4]*lutt6[2];
	// out1[1] += inp1[7]*lutt6[3];
	// out1[1] += inp1[6]*lutt6[0];
	// out1[1] += inp2[1]*lutt6[1]; // 1
	//
	// out1[0] = inp1[3]*lutt6[6];
	// out1[0] += inp1[2]*lutt6[7];
	// out1[0] += inp1[5]*lutt6[4];
	// out1[0] += inp1[4]*lutt6[5];
	// out1[0] += inp1[7]*lutt6[2];
	// out1[0] += inp1[6]*lutt6[3];
	// out1[0] += inp2[1]*lutt6[0];
	// out1[0] += inp2[0]*lutt6[1];
	//
	// out1[3] = inp1[2]*lutt6[6];
	// out1[3] += inp1[5]*lutt6[7];
	// out1[3] += inp1[4]*lutt6[4];
	// out1[3] += inp1[7]*lutt6[5];
	// out1[3] += inp1[6]*lutt6[2];
	// out1[3] += inp2[1]*lutt6[3];
	// out1[3] += inp2[0]*lutt6[0];
	// out1[3] += inp2[3]*lutt6[1];
	//
	// out1[2] = inp1[5]*lutt6[6];
	// out1[2] += inp1[4]*lutt6[7];
	// out1[2] += inp1[7]*lutt6[4];
	// out1[2] += inp1[6]*lutt6[5];
	// out1[2] += inp2[1]*lutt6[2];
	// out1[2] += inp2[0]*lutt6[3];
	// out1[2] += inp2[3]*lutt6[0];
	// out1[2] += inp2[2]*lutt6[1];
	//
	// out1[5] = inp1[4]*lutt6[6];
	// out1[5] += inp1[7]*lutt6[7];
	// out1[5] += inp1[6]*lutt6[4];
	// out1[5] += inp2[1]*lutt6[5];
	// out1[5] += inp2[0]*lutt6[2];
	// out1[5] += inp2[3]*lutt6[3];
	// out1[5] += inp2[2]*lutt6[0];
	// out1[5] += inp2[5]*lutt6[1];
	//
	// out1[4] = inp1[7]*lutt6[6];
	// out1[4] += inp1[6]*lutt6[7];
	// out1[4] += inp2[1]*lutt6[4];
	// out1[4] += inp2[0]*lutt6[5];
	// out1[4] += inp2[3]*lutt6[2];
	// out1[4] += inp2[2]*lutt6[3];
	// out1[4] += inp2[5]*lutt6[0];
	// out1[4] += inp2[4]*lutt6[1];
	//
	// out1[7] = inp1[6]*lutt6[6];
	// out1[7] += inp2[1]*lutt6[7];
	// out1[7] += inp2[0]*lutt6[4];
	// out1[7] += inp2[3]*lutt6[5];
	// out1[7] += inp2[2]*lutt6[2];
	// out1[7] += inp2[5]*lutt6[3];
	// out1[7] += inp2[4]*lutt6[0];
	// out1[7] += inp2[7]*lutt6[1];
	//
	// out1[6] = inp2[1]*lutt6[6];
	// out1[6] += inp2[0]*lutt6[7];
	// out1[6] += inp2[3]*lutt6[4];
	// out1[6] += inp2[2]*lutt6[5];
	// out1[6] += inp2[5]*lutt6[2];
	// out1[6] += inp2[4]*lutt6[3];
	// out1[6] += inp2[7]*lutt6[0];
	// out1[6] += inp2[6]*lutt6[1];
	// outp[1] = /*CLAMP*/((out1[1]+0x4000) >> 0xF);
	// outp[0] = /*CLAMP*/((out1[0]+0x4000) >> 0xF);
	// outp[3] = /*CLAMP*/((out1[3]+0x4000) >> 0xF);
	// outp[2] = /*CLAMP*/((out1[2]+0x4000) >> 0xF);
	// outp[5] = /*CLAMP*/((out1[5]+0x4000) >> 0xF);
	// outp[4] = /*CLAMP*/((out1[4]+0x4000) >> 0xF);
	// outp[7] = /*CLAMP*/((out1[7]+0x4000) >> 0xF);
	// outp[6] = /*CLAMP*/((out1[6]+0x4000) >> 0xF);
	// inp1 = inp2;
	// inp2 += 8;
	// outp += 8;
	// }
	// // memcpy (rdram+(t9&0xFFFFFF), dmem+0xFB0, 0x20);
	// memcpy (save, inp2-8, 0x10);
	// memcpy (BufferSpace+(k0&0xffff), outbuff, cnt);
	// }

	protected OpCode SEGMENT2 = new OpCode()
	{
		public void exec(int inst1, int inst2)
		{
			if (isZeldaABI)
			{
				// FILTER2 ();
				return;
			}
			if ((inst1 & 0xFFFFFF) == 0)
			{
				isMKABI = true;
			}
			else
			{
				isMKABI = false;
				isZeldaABI = true;
				// FILTER2 ();
			}
		}
	};
}
