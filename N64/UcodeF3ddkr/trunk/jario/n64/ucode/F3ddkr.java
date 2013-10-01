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
import jario.n64.ucode.Math3D;

/**
 * F3DDKR is F3D for Diddy Kong Racing
 */
public class F3ddkr extends F3d
{
	protected static final int F3DDKR_VTX_APPEND = 0x00010000;

	protected static final int F3DDKR_DMA_MTX = 0x01;
	protected static final int F3DDKR_DMA_VTX = 0x04;
	protected static final int F3DDKR_DMA_TRI = 0x05;
	protected static final int F3DDKR_DMA_DL = 0x07;
	protected static final int F3DDKR_DMA_OFFSETS = 0xBF;

	public F3ddkr()
	{
	}

	@Override
	public void clock(long ticks)
	{
		gbiInitFlags();

		Gbi.G_DMA_TRI = F3DDKR_DMA_TRI;
		Gbi.G_QUAD = F3d.F3D_QUAD;
		Gbi.G_TRI4 = F3d.F3D_TRI4;

		sp_writeRegister(SP_DLPC_STACK_REG, 10);

		dlist[0x00] = F3D_SPNoOp;
		dlist[0x01] = F3DDKR_DMA_Mtx;
		dlist[0x03] = F3D_MoveMem;
		dlist[0x04] = F3DDKR_DMA_Vtx;
		dlist[0x06] = F3D_DList;
		dlist[0x07] = F3DDKR_DMA_DList;
		dlist[0x05] = F3DDKR_DMA_Tri;

		dlist[0xBF] = F3DDKR_DMA_Offsets;
		dlist[0xBE] = F3D_CullDL;
		dlist[0xBC] = F3DDKR_MoveWord;
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

	// Protected Methods ///////////////////////////////////////////////////////

	protected void gSPSetDMAOffsets(int mtxoffset, int vtxoffset)
	{
		DMAOffsets.mtx = mtxoffset;
		DMAOffsets.vtx = vtxoffset;
	}

	protected void gSPDMATriangles(int address, int n)
	{
		if (address + 16 * n > rdramSize)
			return;

		for (int i = 0; i < n; i++)
		{
			int v0 = rdram.read8bit(address + 1) & 0xFF;
			vertices[v0].tex[0] = rdram16bit.read16bit(address + 4) * Gbi.FIXED2FLOATRECIP5;
			vertices[v0].tex[1] = rdram16bit.read16bit(address + 6) * Gbi.FIXED2FLOATRECIP5;

			int v1 = rdram.read8bit(address + 2) & 0xFF;
			vertices[v1].tex[0] = rdram16bit.read16bit(address + 8) * Gbi.FIXED2FLOATRECIP5;
			vertices[v1].tex[1] = rdram16bit.read16bit(address + 10) * Gbi.FIXED2FLOATRECIP5;

			int v2 = rdram.read8bit(address + 3) & 0xFF;
			vertices[v2].tex[0] = rdram16bit.read16bit(address + 12) * Gbi.FIXED2FLOATRECIP5;
			vertices[v2].tex[1] = rdram16bit.read16bit(address + 14) * Gbi.FIXED2FLOATRECIP5;

			gSPTriangle(v0, v1, v2, 0);
			address += 16;
		}

		gSPFlushTriangles();
		vertexi = 0;
	}

	protected void gSPDMAVertex(int address, int n, int v0, boolean append)
	{
		v0 = vertexi + v0;
		if (append)
		{
			if ((matrix.billboard) != 0)
			{
				vertexi = 1;
			}
		}
		else
		{
			vertexi = 0;
		}

		address = DMAOffsets.vtx + address;

		if ((address + 10 * n) > rdramSize)
			return;

		if ((n + v0) < (80))
		{
			SPVertex vertex;
			for (int i = v0; i < n + v0; i++)
			{
				vertex = vertices[i];
				vertex.vtx[0] = rdram16bit.read16bit(address ^ 2);
				vertex.vtx[1] = rdram16bit.read16bit((address + 2) ^ 2);
				vertex.vtx[2] = rdram16bit.read16bit((address + 4) ^ 2);

				if ((geometryMode & Gbi.G_LIGHTING) != 0)
				{
					vertex.norm[0] = rdram.read8bit((address + 6) ^ 3);
					vertex.norm[1] = rdram.read8bit((address + 7) ^ 3);
					vertex.norm[2] = rdram.read8bit((address + 8) ^ 3);
					vertex.color[3] = (rdram.read8bit((address + 9) ^ 3) & 0xFF) * 0.0039215689f;
				}
				else
				{
					vertex.color[0] = (rdram.read8bit((address + 6) ^ 3) & 0xFF) * 0.0039215689f;
					vertex.color[1] = (rdram.read8bit((address + 7) ^ 3) & 0xFF) * 0.0039215689f;
					vertex.color[2] = (rdram.read8bit((address + 8) ^ 3) & 0xFF) * 0.0039215689f;
					vertex.color[3] = (rdram.read8bit((address + 9) ^ 3) & 0xFF) * 0.0039215689f;
				}

				gSPProcessVertex(vertex);
				address += 10;
			}
		}

		vertexi += n;
	}

	protected void gSPDMAMatrix(int address, int index, boolean multiply)
	{
		address = DMAOffsets.mtx + address;
		if (address + 64 > rdramSize)
			return;

		loadMatrix(tmpmtx, address);
		matrix.modelViewi = index;

		if (multiply)
		{
			Math3D.copyMatrix(matrix.modelView[matrix.modelViewi], matrix.modelView[0]);
			Math3D.multMatrix(matrix.modelView[matrix.modelViewi], tmpmtx);
		}
		else
			Math3D.copyMatrix(matrix.modelView[matrix.modelViewi], tmpmtx);

		Math3D.copyMatrix(matrix.projection, Math3D.IDENTITY_MATRIX);
		changed |= CHANGED_MATRIX;
	}

	protected void gSPMatrixBillboard(int x)
	{
		matrix.billboard = x;
	}

	protected void gSPMatrixModelViewi(int x)
	{
		matrix.modelViewi = x;
		changed |= CHANGED_MATRIX;
	}

	protected void gSPDMADisplayList(int dl, int n)
	{
		if ((dl + (n << 3)) > rdramSize)
			return;

		int pc = sp_readRegister(SP_DLPC_REG);

		sp_writeRegister(SP_DLPC_REG, segmentToPhysical(dl));

		((Bus64bit) sp).write64bit(dl, n & 0xFFFFFFFFL);

		sp_writeRegister(SP_DLPC_REG, pc);
	}

	/************************* OpCode functions *************************/

	protected OpCode F3DDKR_DMA_Mtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			if ((w0 & Gbi.SR_MASK_16) != 64) {
			return;
			}

			int index = (w0 >> 16) & Gbi.SR_MASK_4;
			boolean multiply;

			if (index == 0)
			{ // DKR
				index = (w0 >> 22) & Gbi.SR_MASK_2;
				multiply = false;
			}
			else
			{ // Gemini
				multiply = ((w0 >> 23) & Gbi.SR_MASK_1) != 0;
			}

			gSPDMAMatrix(segmentToPhysical(w1), index, multiply);
		}
	};

	protected OpCode F3DDKR_DMA_Vtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			int n = ((w0 >> 19) & Gbi.SR_MASK_5) + 1;
			gSPDMAVertex(segmentToPhysical(w1), n, (w0 >> 9) & Gbi.SR_MASK_5, (w0 & F3DDKR_VTX_APPEND) != 0);
		}
	};

	protected OpCode F3DDKR_DMA_Tri = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPDMATriangles(segmentToPhysical(w1), (w0 >> 4) & Gbi.SR_MASK_12);
		}
	};

	protected OpCode F3DDKR_DMA_DList = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPDMADisplayList(w1, (w0 >> 16) & Gbi.SR_MASK_8);
		}
	};

	protected OpCode F3DDKR_DMA_Offsets = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPSetDMAOffsets(w0 & Gbi.SR_MASK_24, w1 & Gbi.SR_MASK_24);
		}
	};

	protected OpCode F3DDKR_MoveWord = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			switch (w0 & Gbi.SR_MASK_8)
			{
			case 0x02:
				gSPMatrixBillboard(w1 & 1);
				break;
			case 0x0A:
				gSPMatrixModelViewi((w1 >> 6) & Gbi.SR_MASK_2);
				break;
			default:
				F3D_MoveWord.exec(w0, w1);
				break;
			}
		}
	};
}
