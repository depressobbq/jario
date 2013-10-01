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

import jario.hardware.Bus64bit;
import jario.n64.ucode.F3d;
import jario.n64.ucode.Gbi;

public class F3dex extends F3d
{
	protected static final int F3DEX_MTX_STACKSIZE = 18;

	protected static final int F3DEX_MTX_MODELVIEW = 0x00;
	protected static final int F3DEX_MTX_PROJECTION = 0x01;
	protected static final int F3DEX_MTX_MUL = 0x00;
	protected static final int F3DEX_MTX_LOAD = 0x02;
	protected static final int F3DEX_MTX_NOPUSH = 0x00;
	protected static final int F3DEX_MTX_PUSH = 0x04;

	protected static final int F3DEX_TEXTURE_ENABLE = 0x00000002;
	protected static final int F3DEX_SHADING_SMOOTH = 0x00000200;
	protected static final int F3DEX_CULL_FRONT = 0x00001000;
	protected static final int F3DEX_CULL_BACK = 0x00002000;
	protected static final int F3DEX_CULL_BOTH = 0x00003000;
	protected static final int F3DEX_CLIPPING = 0x00800000;

	protected static final int F3DEX_MV_VIEWPORT = 0x80;

	protected static final int F3DEX_MWO_aLIGHT_1 = 0x00;
	protected static final int F3DEX_MWO_bLIGHT_1 = 0x04;
	protected static final int F3DEX_MWO_aLIGHT_2 = 0x20;
	protected static final int F3DEX_MWO_bLIGHT_2 = 0x24;
	protected static final int F3DEX_MWO_aLIGHT_3 = 0x40;
	protected static final int F3DEX_MWO_bLIGHT_3 = 0x44;
	protected static final int F3DEX_MWO_aLIGHT_4 = 0x60;
	protected static final int F3DEX_MWO_bLIGHT_4 = 0x64;
	protected static final int F3DEX_MWO_aLIGHT_5 = 0x80;
	protected static final int F3DEX_MWO_bLIGHT_5 = 0x84;
	protected static final int F3DEX_MWO_aLIGHT_6 = 0xa0;
	protected static final int F3DEX_MWO_bLIGHT_6 = 0xa4;
	protected static final int F3DEX_MWO_aLIGHT_7 = 0xc0;
	protected static final int F3DEX_MWO_bLIGHT_7 = 0xc4;
	protected static final int F3DEX_MWO_aLIGHT_8 = 0xe0;
	protected static final int F3DEX_MWO_bLIGHT_8 = 0xe4;

	protected static final int F3DEX_MODIFYVTX = 0xB2;
	protected static final int F3DEX_TRI2 = 0xB1;
	protected static final int F3DEX_BRANCH_Z = 0xB0;
	protected static final int F3DEX_LOAD_UCODE = 0xAF; // 0xCF

	public F3dex()
	{
	}

	@Override
	public void clock(long ticks)
	{
		gbiInitFlags();

		Gbi.G_TRI1 = F3d.F3D_TRI1;
		Gbi.G_QUAD = F3d.F3D_QUAD;
		Gbi.G_TRI2 = F3DEX_TRI2;

		sp_writeRegister(SP_DLPC_STACK_REG, 18);

		dlist[0x00] = F3D_SPNoOp;
		dlist[0x01] = F3D_Mtx;
		dlist[0x02] = F3D_Reserved0;
		dlist[0x03] = F3D_MoveMem;
		dlist[0x04] = F3DEX_Vtx;
		dlist[0x05] = F3D_Reserved1;
		dlist[0x06] = F3D_DList;
		dlist[0x07] = F3D_Reserved2;
		dlist[0x08] = F3D_Reserved3;
		dlist[0x09] = F3D_Sprite2D_Base;

		dlist[0xBF] = F3DEX_Tri1;
		dlist[0xBE] = F3DEX_CullDL;
		dlist[0xBD] = F3D_PopMtx;
		dlist[0xBC] = F3D_MoveWord;
		dlist[0xBB] = F3D_Texture;
		dlist[0xBA] = F3D_SetOtherMode_H;
		dlist[0xB9] = F3D_SetOtherMode_L;
		dlist[0xB8] = F3D_EndDL;
		dlist[0xB7] = F3D_SetGeometryMode;
		dlist[0xB6] = F3D_ClearGeometryMode;
		dlist[0xB5] = F3DEX_Quad;
		dlist[0xB4] = F3D_RDPHalf_1;
		dlist[0xB3] = F3D_RDPHalf_2;
		dlist[0xB2] = F3DEX_ModifyVtx;
		dlist[0xB1] = F3DEX_Tri2;
		dlist[0xB0] = F3DEX_Branch_Z;
		dlist[0xAF] = F3DEX_Load_uCode;
	}

	// Protected Methods ///////////////////////////////////////////////////////

	protected static void gbiInitFlags()
	{
		Gbi.G_MTX_PROJECTION = F3DEX_MTX_PROJECTION;
		Gbi.G_MTX_LOAD = F3DEX_MTX_LOAD;
		Gbi.G_MTX_PUSH = F3DEX_MTX_PUSH;
		Gbi.G_CULL_BACK = F3DEX_CULL_BACK;
		Gbi.G_CULL_BOTH = F3DEX_CULL_BOTH;
	}

	protected void gSPBranchLessZ(int address, int vtx, float zval)
	{
		if ((address + 8) > rdramSize)
			return;
		if (vertices[vtx].vtx[2] <= zval)
			sp_writeRegister(SP_DLPC_REG, address);
	}

	protected void gSP2Triangles(int v00, int v01, int v02, int flag0, int v10, int v11, int v12, int flag1)
	{
		gSPTriangle(v00, v01, v02, flag0);
		gSPTriangle(v10, v11, v12, flag1);
		gSPFlushTriangles();
	}

	/************************* OpCode functions *************************/

	protected OpCode F3DEX_Vtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPVertex(segmentToPhysical(w1), (w0 >>> 10) & Gbi.SR_MASK_6, (w0 >>> 17) & Gbi.SR_MASK_7);
		}
	};

	protected OpCode F3DEX_Tri1 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSP1Triangle((w1 >> 17) & Gbi.SR_MASK_7, (w1 >> 9) & Gbi.SR_MASK_7, (w1 >> 1) & Gbi.SR_MASK_7, 0);
		}
	};

	protected OpCode F3DEX_CullDL = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPCullDisplayList((w0 >> 1) & Gbi.SR_MASK_15, (w1 >> 1) & Gbi.SR_MASK_15);
		}
	};

	protected OpCode F3DEX_ModifyVtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPModifyVertex((w0 >> 1) & Gbi.SR_MASK_15, (w0 >> 16) & Gbi.SR_MASK_8, w1);
		}
	};

	protected OpCode F3DEX_Tri2 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSP2Triangles((w0 >> 17) & Gbi.SR_MASK_7, (w0 >> 9) & Gbi.SR_MASK_7, (w0 >> 1) & Gbi.SR_MASK_7, 0,
					(w1 >> 17) & Gbi.SR_MASK_7, (w1 >> 9) & Gbi.SR_MASK_7, (w1 >> 1) & Gbi.SR_MASK_7, 0);
		}
	};

	protected OpCode F3DEX_Quad = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSP1Quadrangle((w1 >> 25) & Gbi.SR_MASK_7, (w1 >> 17) & Gbi.SR_MASK_7, (w1 >> 9) & Gbi.SR_MASK_7, (w1 >> 1) & Gbi.SR_MASK_7);
		}
	};

	protected OpCode F3DEX_Branch_Z = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPBranchLessZ(segmentToPhysical(half_1), (w0 >> 1) & Gbi.SR_MASK_11, w1);
		}
	};

	protected OpCode F3DEX_Load_uCode = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			matrix.modelViewi = 0;
			changed |= CHANGED_MATRIX;
			((Bus64bit) sp).write64bit(half_1, ((1L << 63) | (((w0 & Gbi.SR_MASK_16) + 1) << 32) | (w1 & 0xFFFFFFFFL)));
		}
	};
}
