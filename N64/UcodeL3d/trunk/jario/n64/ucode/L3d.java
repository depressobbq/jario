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

package jario.n64.ucode;

import jario.hardware.BusDMA;
import jario.n64.ucode.F3d;
import jario.n64.ucode.Gbi;

public class L3d extends F3d
{
	protected static final int L3D_LINE3D = 0xB5;

	public L3d()
	{
	}

	@Override
	public void clock(long ticks)
	{
		gbiInitFlags();

		sp_writeRegister(SP_DLPC_STACK_REG, 10);

		dlist[0x00] = F3D_SPNoOp;
		dlist[0x01] = F3D_Mtx;
		dlist[0x02] = F3D_Reserved0;
		dlist[0x03] = F3D_MoveMem;
		dlist[0x04] = F3D_Vtx;
		dlist[0x05] = F3D_Reserved1;
		dlist[0x06] = F3D_DList;
		dlist[0x07] = F3D_Reserved2;
		dlist[0x08] = F3D_Reserved3;
		dlist[0x09] = F3D_Sprite2D_Base;

		dlist[0xBE] = F3D_CullDL;
		dlist[0xBD] = F3D_PopMtx;
		dlist[0xBC] = F3D_MoveWord;
		dlist[0xBB] = F3D_Texture;
		dlist[0xBA] = F3D_SetOtherMode_H;
		dlist[0xB9] = F3D_SetOtherMode_L;
		dlist[0xB8] = F3D_EndDL;
		dlist[0xB7] = F3D_SetGeometryMode;
		dlist[0xB6] = F3D_ClearGeometryMode;
		dlist[0xB5] = L3D_Line3D;
		dlist[0xB4] = F3D_RDPHalf_1;
		dlist[0xB3] = F3D_RDPHalf_2;
		dlist[0xB2] = F3D_RDPHalf_Cont;
	}

	// Protected Methods ///////////////////////////////////////////////////////

	protected void gSPLine3D(int v0, int v1, int flag)
	{
		updateStates();

		int w1 = Float.floatToIntBits(vertices[v0].vtx[0]);
		int w2 = Float.floatToIntBits(vertices[v0].vtx[1]);
		int w3 = Float.floatToIntBits(vertices[v0].vtx[2]);
		int w4 = Float.floatToIntBits(vertices[v0].vtx[3]);

		int w5 = Float.floatToIntBits(vertices[v0].color[0]);
		int w6 = Float.floatToIntBits(vertices[v0].color[1]);
		int w7 = Float.floatToIntBits(vertices[v0].color[2]);
		int w8 = Float.floatToIntBits(vertices[v0].color[3]);

		int w9 = Float.floatToIntBits(vertices[v1].vtx[0]);
		int w10 = Float.floatToIntBits(vertices[v1].vtx[1]);
		int w11 = Float.floatToIntBits(vertices[v1].vtx[2]);
		int w12 = Float.floatToIntBits(vertices[v1].vtx[3]);

		int w13 = Float.floatToIntBits(vertices[v1].color[0]);
		int w14 = Float.floatToIntBits(vertices[v1].color[1]);
		int w15 = Float.floatToIntBits(vertices[v1].color[2]);
		int w16 = Float.floatToIntBits(vertices[v1].color[3]);

		int w17 = Float.floatToIntBits(1.5f);

		dpcmd[0] = 0x11;
		dpcmd[4] = (byte) (w1 >> 24);
		dpcmd[5] = (byte) (w1 >> 16);
		dpcmd[6] = (byte) (w1 >> 8);
		dpcmd[7] = (byte) (w1 >> 0);
		dpcmd[8] = (byte) (w2 >> 24);
		dpcmd[9] = (byte) (w2 >> 16);
		dpcmd[10] = (byte) (w2 >> 8);
		dpcmd[11] = (byte) (w2 >> 0);
		dpcmd[12] = (byte) (w3 >> 24);
		dpcmd[13] = (byte) (w3 >> 16);
		dpcmd[14] = (byte) (w3 >> 8);
		dpcmd[15] = (byte) (w3 >> 0);
		dpcmd[16] = (byte) (w4 >> 24);
		dpcmd[17] = (byte) (w4 >> 16);
		dpcmd[18] = (byte) (w4 >> 8);
		dpcmd[19] = (byte) (w4 >> 0);
		dpcmd[20] = (byte) (w5 >> 24);
		dpcmd[21] = (byte) (w5 >> 16);
		dpcmd[22] = (byte) (w5 >> 8);
		dpcmd[23] = (byte) (w5 >> 0);
		dpcmd[24] = (byte) (w6 >> 24);
		dpcmd[25] = (byte) (w6 >> 16);
		dpcmd[26] = (byte) (w6 >> 8);
		dpcmd[27] = (byte) (w6 >> 0);
		dpcmd[28] = (byte) (w7 >> 24);
		dpcmd[29] = (byte) (w7 >> 16);
		dpcmd[30] = (byte) (w7 >> 8);
		dpcmd[31] = (byte) (w7 >> 0);
		dpcmd[32] = (byte) (w8 >> 24);
		dpcmd[33] = (byte) (w8 >> 16);
		dpcmd[34] = (byte) (w8 >> 8);
		dpcmd[35] = (byte) (w8 >> 0);
		dpcmd[36] = (byte) (w9 >> 24);
		dpcmd[37] = (byte) (w9 >> 16);
		dpcmd[38] = (byte) (w9 >> 8);
		dpcmd[39] = (byte) (w9 >> 0);
		dpcmd[40] = (byte) (w10 >> 24);
		dpcmd[41] = (byte) (w10 >> 16);
		dpcmd[42] = (byte) (w10 >> 8);
		dpcmd[43] = (byte) (w10 >> 0);
		dpcmd[44] = (byte) (w11 >> 24);
		dpcmd[45] = (byte) (w11 >> 16);
		dpcmd[46] = (byte) (w11 >> 8);
		dpcmd[47] = (byte) (w11 >> 0);
		dpcmd[48] = (byte) (w12 >> 24);
		dpcmd[49] = (byte) (w12 >> 16);
		dpcmd[50] = (byte) (w12 >> 8);
		dpcmd[51] = (byte) (w12 >> 0);
		dpcmd[52] = (byte) (w13 >> 24);
		dpcmd[53] = (byte) (w13 >> 16);
		dpcmd[54] = (byte) (w13 >> 8);
		dpcmd[55] = (byte) (w13 >> 0);
		dpcmd[56] = (byte) (w14 >> 24);
		dpcmd[57] = (byte) (w14 >> 16);
		dpcmd[58] = (byte) (w14 >> 8);
		dpcmd[59] = (byte) (w14 >> 0);
		dpcmd[60] = (byte) (w15 >> 24);
		dpcmd[61] = (byte) (w15 >> 16);
		dpcmd[62] = (byte) (w15 >> 8);
		dpcmd[63] = (byte) (w15 >> 0);
		dpcmd[64] = (byte) (w16 >> 24);
		dpcmd[65] = (byte) (w16 >> 16);
		dpcmd[66] = (byte) (w16 >> 8);
		dpcmd[67] = (byte) (w16 >> 0);
		dpcmd[68] = (byte) (w17 >> 24);
		dpcmd[69] = (byte) (w17 >> 16);
		dpcmd[70] = (byte) (w17 >> 8);
		dpcmd[71] = (byte) (w17 >> 0);
		((BusDMA) dp).writeDMA(0x11, dpcmdbuf, 0, 72);
	}

	protected void gSPLineW3D(int v0, int v1, int wd, int flag)
	{
		updateStates();

		int w1 = Float.floatToIntBits(vertices[v0].vtx[0]);
		int w2 = Float.floatToIntBits(vertices[v0].vtx[1]);
		int w3 = Float.floatToIntBits(vertices[v0].vtx[2]);
		int w4 = Float.floatToIntBits(vertices[v0].vtx[3]);

		int w5 = Float.floatToIntBits(vertices[v0].color[0]);
		int w6 = Float.floatToIntBits(vertices[v0].color[1]);
		int w7 = Float.floatToIntBits(vertices[v0].color[2]);
		int w8 = Float.floatToIntBits(vertices[v0].color[3]);

		int w9 = Float.floatToIntBits(vertices[v1].vtx[0]);
		int w10 = Float.floatToIntBits(vertices[v1].vtx[1]);
		int w11 = Float.floatToIntBits(vertices[v1].vtx[2]);
		int w12 = Float.floatToIntBits(vertices[v1].vtx[3]);

		int w13 = Float.floatToIntBits(vertices[v1].color[0]);
		int w14 = Float.floatToIntBits(vertices[v1].color[1]);
		int w15 = Float.floatToIntBits(vertices[v1].color[2]);
		int w16 = Float.floatToIntBits(vertices[v1].color[3]);

		int w17 = Float.floatToIntBits(1.5f + wd * 0.5f);

		dpcmd[0] = 0x11;
		dpcmd[4] = (byte) (w1 >> 24);
		dpcmd[5] = (byte) (w1 >> 16);
		dpcmd[6] = (byte) (w1 >> 8);
		dpcmd[7] = (byte) (w1 >> 0);
		dpcmd[8] = (byte) (w2 >> 24);
		dpcmd[9] = (byte) (w2 >> 16);
		dpcmd[10] = (byte) (w2 >> 8);
		dpcmd[11] = (byte) (w2 >> 0);
		dpcmd[12] = (byte) (w3 >> 24);
		dpcmd[13] = (byte) (w3 >> 16);
		dpcmd[14] = (byte) (w3 >> 8);
		dpcmd[15] = (byte) (w3 >> 0);
		dpcmd[16] = (byte) (w4 >> 24);
		dpcmd[17] = (byte) (w4 >> 16);
		dpcmd[18] = (byte) (w4 >> 8);
		dpcmd[19] = (byte) (w4 >> 0);
		dpcmd[20] = (byte) (w5 >> 24);
		dpcmd[21] = (byte) (w5 >> 16);
		dpcmd[22] = (byte) (w5 >> 8);
		dpcmd[23] = (byte) (w5 >> 0);
		dpcmd[24] = (byte) (w6 >> 24);
		dpcmd[25] = (byte) (w6 >> 16);
		dpcmd[26] = (byte) (w6 >> 8);
		dpcmd[27] = (byte) (w6 >> 0);
		dpcmd[28] = (byte) (w7 >> 24);
		dpcmd[29] = (byte) (w7 >> 16);
		dpcmd[30] = (byte) (w7 >> 8);
		dpcmd[31] = (byte) (w7 >> 0);
		dpcmd[32] = (byte) (w8 >> 24);
		dpcmd[33] = (byte) (w8 >> 16);
		dpcmd[34] = (byte) (w8 >> 8);
		dpcmd[35] = (byte) (w8 >> 0);
		dpcmd[36] = (byte) (w9 >> 24);
		dpcmd[37] = (byte) (w9 >> 16);
		dpcmd[38] = (byte) (w9 >> 8);
		dpcmd[39] = (byte) (w9 >> 0);
		dpcmd[40] = (byte) (w10 >> 24);
		dpcmd[41] = (byte) (w10 >> 16);
		dpcmd[42] = (byte) (w10 >> 8);
		dpcmd[43] = (byte) (w10 >> 0);
		dpcmd[44] = (byte) (w11 >> 24);
		dpcmd[45] = (byte) (w11 >> 16);
		dpcmd[46] = (byte) (w11 >> 8);
		dpcmd[47] = (byte) (w11 >> 0);
		dpcmd[48] = (byte) (w12 >> 24);
		dpcmd[49] = (byte) (w12 >> 16);
		dpcmd[50] = (byte) (w12 >> 8);
		dpcmd[51] = (byte) (w12 >> 0);
		dpcmd[52] = (byte) (w13 >> 24);
		dpcmd[53] = (byte) (w13 >> 16);
		dpcmd[54] = (byte) (w13 >> 8);
		dpcmd[55] = (byte) (w13 >> 0);
		dpcmd[56] = (byte) (w14 >> 24);
		dpcmd[57] = (byte) (w14 >> 16);
		dpcmd[58] = (byte) (w14 >> 8);
		dpcmd[59] = (byte) (w14 >> 0);
		dpcmd[60] = (byte) (w15 >> 24);
		dpcmd[61] = (byte) (w15 >> 16);
		dpcmd[62] = (byte) (w15 >> 8);
		dpcmd[63] = (byte) (w15 >> 0);
		dpcmd[64] = (byte) (w16 >> 24);
		dpcmd[65] = (byte) (w16 >> 16);
		dpcmd[66] = (byte) (w16 >> 8);
		dpcmd[67] = (byte) (w16 >> 0);
		dpcmd[68] = (byte) (w17 >> 24);
		dpcmd[69] = (byte) (w17 >> 16);
		dpcmd[70] = (byte) (w17 >> 8);
		dpcmd[71] = (byte) (w17 >> 0);
		((BusDMA) dp).writeDMA(0x11, dpcmdbuf, 0, 72);
	}

	/************************* OpCode functions *************************/

	protected OpCode L3D_Line3D = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			int wd = w1 & Gbi.SR_MASK_8;
			if (wd == 0)
				gSPLine3D(((w1 >> 16) & Gbi.SR_MASK_8) / 10, ((w1 >> 8) & Gbi.SR_MASK_8) / 10, (w1 >> 24) & Gbi.SR_MASK_8);
			else
				gSPLineW3D(((w1 >> 16) & Gbi.SR_MASK_8) / 10, ((w1 >> 8) & Gbi.SR_MASK_8) / 10, wd, (w1 >> 24) & Gbi.SR_MASK_8);
		}
	};
}
