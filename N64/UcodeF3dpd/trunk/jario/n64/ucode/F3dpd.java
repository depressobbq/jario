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

import jario.n64.ucode.F3d;
import jario.n64.ucode.F3ddkr;
import jario.n64.ucode.Gbi;

/**
 * F3DPD is F3D for Perfect Dark
 */
public class F3dpd extends F3ddkr
{
	protected static final int F3DPD_VTXCOLORBASE = 0x07;

	public F3dpd()
	{
	}

	@Override
	public void clock(long ticks)
	{
		gbiInitFlags();

		Gbi.G_TRI1 = F3d.F3D_TRI1;
		Gbi.G_QUAD = F3d.F3D_QUAD;
		Gbi.G_TRI4 = F3d.F3D_TRI4;

		sp_writeRegister(SP_DLPC_STACK_REG, 10);

		dlist[0x00] = F3D_SPNoOp;
		dlist[0x01] = F3D_Mtx;
		dlist[0x02] = F3D_Reserved0;
		dlist[0x03] = F3D_MoveMem;
		dlist[0x04] = F3DPD_Vtx;
		dlist[0x05] = F3D_Reserved1;
		dlist[0x06] = F3D_DList;
		dlist[0x07] = F3DPD_VtxColorBase;
		dlist[0x08] = F3D_Reserved3;
		dlist[0x09] = F3D_Sprite2D_Base;

		dlist[0xBF] = F3D_Tri1;
		dlist[0xBE] = F3D_CullDL;
		dlist[0xBD] = F3D_PopMtx;
		dlist[0xBC] = F3D_MoveWord;
		dlist[0xBB] = F3D_Texture;
		dlist[0xBA] = F3D_SetOtherMode_H;
		dlist[0xB9] = F3D_SetOtherMode_L;
		dlist[0xB8] = F3D_EndDL;
		dlist[0xB7] = F3D_SetGeometryMode;
		dlist[0xB6] = F3D_ClearGeometryMode;
		dlist[0xB5] = F3D_Quad;
		dlist[0xB4] = F3D_RDPHalf_1;
		dlist[0xB3] = F3D_RDPHalf_2;
		dlist[0xB2] = F3D_RDPHalf_Cont;
		dlist[0xB1] = F3D_Tri4;

		gSPSetDMAOffsets(0, 0);
	}

	/************************* OpCode functions *************************/

	protected OpCode F3DPD_Vtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3DPD_VtxColorBase = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};
}
