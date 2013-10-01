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

import jario.n64.ucode.F3dex2;

public class S2dex2 extends F3dex2
{
	protected static final int S2DEX2_OBJ_RECTANGLE_R = 0xDA;
	protected static final int S2DEX2_OBJ_MOVEMEM = 0xDC;
	protected static final int S2DEX2_RDPHALF_0 = 0xE4;
	protected static final int S2DEX2_OBJ_RECTANGLE = 0x01;
	protected static final int S2DEX2_OBJ_SPRITE = 0x02;
	protected static final int S2DEX2_SELECT_DL = 0x04;
	protected static final int S2DEX2_OBJ_LOADTXTR = 0x05;
	protected static final int S2DEX2_OBJ_LDTX_SPRITE = 0x06;
	protected static final int S2DEX2_OBJ_LDTX_RECT = 0x07;
	protected static final int S2DEX2_OBJ_LDTX_RECT_R = 0x08;
	protected static final int S2DEX2_BG_1CYC = 0x09;
	protected static final int S2DEX2_BG_COPY = 0x0A;
	protected static final int S2DEX2_OBJ_RENDERMODE = 0x0B;

	public S2dex2()
	{
	}

	@Override
	public void clock(long ticks)
	{
		gbiInitFlags();

		geometryMode = 0;
		sp_writeRegister(SP_DLPC_STACK_REG, 18);

		dlist[0xE0] = F3D_SPNoOp;
		dlist[0x09] = S2DEX2_BG_1Cyc;
		dlist[0x0A] = S2DEX2_BG_Copy;
		dlist[0x01] = S2DEX2_Obj_Rectangle;
		dlist[0x02] = S2DEX2_Obj_Sprite;
		dlist[0xDC] = S2DEX2_Obj_MoveMem;
		dlist[0xDE] = F3D_DList;
		dlist[0x04] = S2DEX2_Select_DL;
		dlist[0x0B] = S2DEX2_Obj_RenderMode;
		dlist[0xDA] = S2DEX2_Obj_Rectangle_R;
		dlist[0x05] = S2DEX2_Obj_LoadTxtr;
		dlist[0x06] = S2DEX2_Obj_LdTx_Sprite;
		dlist[0x07] = S2DEX2_Obj_LdTx_Rect;
		dlist[0x08] = S2DEX2_Obj_LdTx_Rect_R;
		dlist[0xDB] = F3DEX2_MoveWord;
		dlist[0xE3] = F3DEX2_SetOtherMode_H;
		dlist[0xE2] = F3DEX2_SetOtherMode_L;
		dlist[0xDF] = F3D_EndDL;
		dlist[0xE1] = F3D_RDPHalf_1;
		dlist[0xF1] = F3D_RDPHalf_2;
		dlist[0xDD] = F3DEX_Load_uCode;
	}

	/************************* OpCode functions *************************/

	protected OpCode S2DEX2_BG_1Cyc = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_BG_Copy = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_Obj_Rectangle = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_Obj_Sprite = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_Obj_MoveMem = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_Select_DL = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_Obj_RenderMode = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_Obj_Rectangle_R = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_Obj_LoadTxtr = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_Obj_LdTx_Sprite = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_Obj_LdTx_Rect = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX2_Obj_LdTx_Rect_R = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};
}
