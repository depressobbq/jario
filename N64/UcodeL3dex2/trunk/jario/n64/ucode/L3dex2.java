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
import jario.n64.ucode.F3dex2;
import jario.n64.ucode.Gbi;

public class L3dex2 extends F3dex2
{
	protected static final int L3DEX2_LINE3D = 0x08;

	public L3dex2()
	{
	}

	@Override
	public void clock(long ticks)
	{
		gbiInitFlags();

		sp_writeRegister(SP_DLPC_STACK_REG, 18);

		dlist[0xF1] = F3D_RDPHalf_2;
		dlist[0xE3] = F3DEX2_SetOtherMode_H;
		dlist[0xE2] = F3DEX2_SetOtherMode_L;
		dlist[0xE1] = F3D_RDPHalf_1;
		dlist[0xE0] = F3D_SPNoOp;
		dlist[0xDF] = F3D_EndDL;
		dlist[0xDE] = F3D_DList;
		dlist[0xDD] = F3DEX_Load_uCode;
		dlist[0xDC] = F3DEX2_MoveMem;
		dlist[0xDB] = F3DEX2_MoveWord;
		dlist[0xDA] = F3DEX2_Mtx;
		dlist[0xD9] = F3DEX2_GeometryMode;
		dlist[0xD8] = F3DEX2_PopMtx;
		dlist[0xD7] = F3DEX2_Texture;
		dlist[0xD6] = F3DEX2_DMAIO;
		dlist[0xD5] = F3DEX2_Special_1;
		dlist[0xD4] = F3DEX2_Special_2;
		dlist[0xD3] = F3DEX2_Special_3;

		dlist[0x01] = F3DEX2_Vtx;
		dlist[0x02] = F3DEX_ModifyVtx;
		dlist[0x03] = F3DEX_CullDL;
		dlist[0x04] = F3DEX_Branch_Z;

		dlist[0x08] = L3DEX2_Line3D;
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

		dpcmd[0] = 0x011;
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

		dpcmd[0] = 0x011;
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

	protected OpCode L3DEX2_Line3D = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			int wd = w0 & Gbi.SR_MASK_8;
			if (wd == 0)
				gSPLine3D((w0 >> 17) & Gbi.SR_MASK_7, (w0 >> 9) & Gbi.SR_MASK_7, 0);
			else
				gSPLineW3D((w0 >> 17) & Gbi.SR_MASK_7, (w0 >> 9) & Gbi.SR_MASK_7, wd, 0);
		}
	};
}
