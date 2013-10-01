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

import jario.n64.ucode.F3dex;

public class S2dex extends F3dex
{
	protected static final int G_BGLT_LOADBLOCK = 0x0033;
	protected static final int G_BGLT_LOADTILE = 0xfff4;

	protected static final int G_BG_FLAG_FLIPS = 0x01;
	protected static final int G_BG_FLAG_FLIPT = 0x10;

	protected static final int S2DEX_BG_1CYC = 0x01;
	protected static final int S2DEX_BG_COPY = 0x02;
	protected static final int S2DEX_OBJ_RECTANGLE = 0x03;
	protected static final int S2DEX_OBJ_SPRITE = 0x04;
	protected static final int S2DEX_OBJ_MOVEMEM = 0x05;
	protected static final int S2DEX_LOAD_UCODE = 0xAF;
	protected static final int S2DEX_SELECT_DL = 0xB0;
	protected static final int S2DEX_OBJ_RENDERMODE = 0xB1;
	protected static final int S2DEX_OBJ_RECTANGLE_R = 0xB2;
	protected static final int S2DEX_OBJ_LOADTXTR = 0xC1;
	protected static final int S2DEX_OBJ_LDTX_SPRITE = 0xC2;
	protected static final int S2DEX_OBJ_LDTX_RECT = 0xC3;
	protected static final int S2DEX_OBJ_LDTX_RECT_R = 0xC4;
	protected static final int S2DEX_RDPHALF_0 = 0xE4;

	public S2dex()
	{
	}

	@Override
	public void clock(long ticks)
	{
		gbiInitFlags();

		geometryMode = 0;
		sp_writeRegister(SP_DLPC_STACK_REG, 18);

		dlist[0x00] = F3D_SPNoOp;
		dlist[0x01] = S2DEX_BG_1Cyc;
		dlist[0x02] = S2DEX_BG_Copy;
		dlist[0x03] = S2DEX_Obj_Rectangle;
		dlist[0x04] = S2DEX_Obj_Sprite;
		dlist[0x05] = S2DEX_Obj_MoveMem;
		dlist[0x06] = F3D_DList;
		dlist[0xB0] = S2DEX_Select_DL;
		dlist[0xB1] = S2DEX_Obj_RenderMode;
		dlist[0xB2] = S2DEX_Obj_Rectangle_R;
		dlist[0xC1] = S2DEX_Obj_LoadTxtr;
		dlist[0xC2] = S2DEX_Obj_LdTx_Sprite;
		dlist[0xC3] = S2DEX_Obj_LdTx_Rect;
		dlist[0xC4] = S2DEX_Obj_LdTx_Rect_R;
		dlist[0xBC] = F3D_MoveWord;
		dlist[0xBA] = F3D_SetOtherMode_H;
		dlist[0xB9] = F3D_SetOtherMode_L;
		dlist[0xB8] = F3D_EndDL;
		dlist[0xB4] = F3D_RDPHalf_1;
		dlist[0xB3] = F3D_RDPHalf_2;
		dlist[0xAF] = F3DEX_Load_uCode;
	}

	/************************* OpCode functions *************************/

	protected OpCode S2DEX_BG_1Cyc = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_BG_Copy = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_Obj_Rectangle = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_Obj_Sprite = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_Obj_MoveMem = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_Select_DL = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_Obj_RenderMode = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_Obj_Rectangle_R = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_Obj_LoadTxtr = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_Obj_LdTx_Sprite = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_Obj_LdTx_Rect = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode S2DEX_Obj_LdTx_Rect_R = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};
}
