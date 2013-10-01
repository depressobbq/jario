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
import jario.n64.ucode.Gbi;
import jario.n64.ucode.Math3D;

public class F3dexbg extends F3dex2
{
	public F3dexbg()
	{
	}

	@Override
	public void clock(long ticks)
	{
		gbiInitFlags();

		Gbi.G_TRI1 = F3dex2.F3DEX2_TRI1;
		Gbi.G_TRI2 = F3dex2.F3DEX2_TRI2;
		Gbi.G_QUAD = F3dex2.F3DEX2_QUAD;

		sp_writeRegister(SP_DLPC_STACK_REG, 18);

		dlist[0xF1] = F3D_RDPHalf_2;
		dlist[0xE3] = F3DEX2_SetOtherMode_H;
		dlist[0xE2] = F3DEX2_SetOtherMode_L;
		dlist[0xE1] = F3D_RDPHalf_1;
		dlist[0xE0] = F3D_SPNoOp;
		dlist[0xDF] = F3D_EndDL;
		dlist[0xDE] = F3D_DList;
		dlist[0xDD] = F3DEX_Load_uCode;
		dlist[0xDC] = F3DEXBG_MoveMem;
		dlist[0xDB] = F3DEXBG_MoveWord;
		dlist[0xDA] = F3DEX2_Mtx;
		dlist[0xD9] = F3DEX2_GeometryMode;
		dlist[0xD8] = F3DEX2_PopMtx;
		dlist[0xD7] = F3DEX2_Texture;
		dlist[0xD6] = F3DEX2_DMAIO;
		dlist[0xD5] = F3DEX2_Special_1;
		dlist[0xD4] = F3DEX2_Special_2;
		dlist[0xD3] = F3DEX2_Special_3;

		dlist[0x01] = F3DEXBG_Vtx;
		dlist[0x02] = F3DEX_ModifyVtx;
		dlist[0x03] = F3DEX_CullDL;
		dlist[0x04] = F3DEX_Branch_Z;
		dlist[0x05] = F3DEX2_Tri1;
		dlist[0x06] = F3DEX_Tri2;
		dlist[0x07] = F3DEX2_Quad;

		for (int i = 0x10; i < 0x20; i++)
			dlist[i] = F3DEXBG_Tri4;
	}

	// Protected Methods ///////////////////////////////////////////////////////

	protected void gSPNormals(int address)
	{
		uc8_normale_addr = address;
	}

	protected void gSPCoordMod(int coord, int index, int pos)
	{
		if (pos == 0)
		{
			uc8_coord_mod[0 + index] = (short) (coord >> 16);
			uc8_coord_mod[1 + index] = (short) (coord & 0xffff);
		}
		else if (pos == 0x10)
		{
			uc8_coord_mod[4 + index] = (coord >> 16) / 65536.0f;
			uc8_coord_mod[5 + index] = (coord & 0xffff) / 65536.0f;
			uc8_coord_mod[12 + index] = uc8_coord_mod[0 + index] + uc8_coord_mod[4 + index];
			uc8_coord_mod[13 + index] = uc8_coord_mod[1 + index] + uc8_coord_mod[5 + index];
		}
		else if (pos == 0x20)
		{
			uc8_coord_mod[8 + index] = (short) (coord >> 16);
			uc8_coord_mod[9 + index] = (short) (coord & 0xffff);
		}
	}

	protected void gSPLightBg(int address, int n)
	{
		n -= 2;
		if (n >= 0)
		{
			int col = rdram.read8bit(address + 0) & 0xFF;
			lights[n].color[0] = (float) col / 255.0f;
			lights[n].nonblack = col;
			col = rdram.read8bit(address + 1) & 0xFF;
			lights[n].color[1] = (float) col / 255.0f;
			lights[n].nonblack += col;
			col = rdram.read8bit(address + 2) & 0xFF;
			lights[n].color[2] = (float) col / 255.0f;
			lights[n].nonblack += col;
			lights[n].a = 1.0f;
			lights[n].vec[0] = (float) rdram.read8bit(address + 8) / 127.0f;
			lights[n].vec[1] = (float) rdram.read8bit(address + 9) / 127.0f;
			lights[n].vec[2] = (float) rdram.read8bit(address + 10) / 127.0f;
			lights[n].x = (float) rdram16bit.read16bit(address + 32);
			lights[n].y = (float) rdram16bit.read16bit(address + 34);
			lights[n].z = (float) rdram16bit.read16bit(address + 36);
			lights[n].w = (float) rdram16bit.read16bit(address + 38);
			lights[n].nonzero = rdram.read8bit(address + 12);
			lights[n].ca = (float) lights[n].nonzero / 16.0f;
		}
	}

	protected void gSPVertexBg(int address, int n, int v0)
	{
		if ((changed & CHANGED_MATRIX) != 0)
			gSPCombineMatrices();

		if (v0 - n < 0)
			return;

		for (int i = 0; i < (n << 4); i += 16)
		{
			SPVertex vertex = vertices[v0 - n + (i >> 4)];
			vertex.vtx[0] = (float) rdram16bit.read16bit((address + i) + 0);
			vertex.vtx[1] = (float) rdram16bit.read16bit((address + i) + 2);
			vertex.vtx[2] = (float) rdram16bit.read16bit((address + i) + 4);
			vertex.flag = rdram16bit.read16bit((address + i) + 6);
			vertex.tex[0] = (float) rdram16bit.read16bit((address + i) + 8) * 0.03125f;
			vertex.tex[1] = (float) rdram16bit.read16bit((address + i) + 10) * 0.03125f;
			vertex.color[3] = (rdram.read8bit((address + i) + 15) & 0xFF) * 0.0039215689f;

			Math3D.transformVertex(vertex.vtx, matrix.combined);

			if (vertex.vtx[0] < -vertex.vtx[3])
				vertex.clip[0] = -1.0f;
			else if (vertex.vtx[0] > vertex.vtx[3])
				vertex.clip[0] = 1.0f;
			else
				vertex.clip[0] = 0.0f;

			if (vertex.vtx[1] < -vertex.vtx[3])
				vertex.clip[1] = -1.0f;
			else if (vertex.vtx[1] > vertex.vtx[3])
				vertex.clip[1] = 1.0f;
			else
				vertex.clip[1] = 0.0f;

			if (vertex.vtx[3] <= 0.0f)
				vertex.clip[2] = -1.0f;
			else if (vertex.vtx[2] < -vertex.vtx[3])
				vertex.clip[2] = -0.1f;
			else if (vertex.vtx[2] > vertex.vtx[3])
				vertex.clip[2] = 1.0f;
			else
				vertex.clip[2] = 0.0f;

			vertex.color[0] = (rdram.read8bit((address + i) + 12) & 0xFF) * 0.0039215689f;
			vertex.color[1] = (rdram.read8bit((address + i) + 13) & 0xFF) * 0.0039215689f;
			vertex.color[2] = (rdram.read8bit((address + i) + 14) & 0xFF) * 0.0039215689f;

			if ((geometryMode & 0x00020000 /* Gbi.G_LIGHTING */) != 0)
			{
				int shift = (v0 - n) << 1;
				vertex.norm[0] = rdram.read8bit(uc8_normale_addr + (i >> 3) + shift + 0);
				vertex.norm[1] = rdram.read8bit(uc8_normale_addr + (i >> 3) + shift + 1);
				vertex.norm[2] = (byte) (vertex.flag & 0xff);

				Math3D.transformVector(vertex.norm, matrix.modelView[matrix.modelViewi]);
				Math3D.normalize(vertex.norm);
				if ((geometryMode & 0x00080000 /* Gbi.G_TEXTURE_GEN_LINEAR */) != 0)
				{
					vertex.tex[0] = (float) StrictMath.acos(vertex.norm[0]) * 325.94931f;
					vertex.tex[1] = (float) StrictMath.acos(vertex.norm[1]) * 325.94931f;
				}
				else
				{ // G_TEXTURE_GEN
					vertex.tex[0] = (vertex.norm[0] + 1.0f) * 512.0f;
					vertex.tex[1] = (vertex.norm[1] + 1.0f) * 512.0f;
				}

				float[] color = { lights[numLights].color[0], lights[numLights].color[1], lights[numLights].color[2] };
				float light_intensity = 0.0f;
				int l;
				if ((geometryMode & 0x00400000) != 0)
				{
					Math3D.normalize(vertex.norm);
					for (l = 0; l < numLights - 1; l++)
					{
						if (lights[l].nonblack == 0)
							continue;
						light_intensity = Math3D.dotProduct(vertex.norm, lights[l].vec);
						if (light_intensity < 0.0f)
							continue;
						if (lights[l].ca > 0.0f)
						{
							float vx = (vertex.vtx[0] + uc8_coord_mod[8]) * uc8_coord_mod[12] - lights[l].x;
							float vy = (vertex.vtx[1] + uc8_coord_mod[9]) * uc8_coord_mod[13] - lights[l].y;
							float vz = (vertex.vtx[2] + uc8_coord_mod[10]) * uc8_coord_mod[14] - lights[l].z;
							float vw = (vertex.vtx[3] + uc8_coord_mod[11]) * uc8_coord_mod[15] - lights[l].w;
							float len = (vx * vx + vy * vy + vz * vz + vw * vw) / 65536.0f;
							float p_i = lights[l].ca / len;
							if (p_i > 1.0f) p_i = 1.0f;
							light_intensity *= p_i;
						}
						color[0] += lights[l].color[0] * light_intensity;
						color[1] += lights[l].color[1] * light_intensity;
						color[2] += lights[l].color[2] * light_intensity;
					}
					light_intensity = Math3D.dotProduct(vertex.norm, lights[l].vec);
					if (light_intensity > 0.0f)
					{
						color[0] += lights[l].color[0] * light_intensity;
						color[1] += lights[l].color[1] * light_intensity;
						color[2] += lights[l].color[2] * light_intensity;
					}
				}
				else
				{
					for (l = 0; l < numLights; l++)
					{
						if (lights[l].nonblack != 0 && lights[l].nonzero != 0)
						{
							float vx = (vertex.vtx[0] + uc8_coord_mod[8]) * uc8_coord_mod[12] - lights[l].x;
							float vy = (vertex.vtx[1] + uc8_coord_mod[9]) * uc8_coord_mod[13] - lights[l].y;
							float vz = (vertex.vtx[2] + uc8_coord_mod[10]) * uc8_coord_mod[14] - lights[l].z;
							float vw = (vertex.vtx[3] + uc8_coord_mod[11]) * uc8_coord_mod[15] - lights[l].w;
							float len = (vx * vx + vy * vy + vz * vz + vw * vw) / 65536.0f;
							light_intensity = lights[l].ca / len;
							if (light_intensity > 1.0f) light_intensity = 1.0f;
							color[0] += lights[l].color[0] * light_intensity;
							color[1] += lights[l].color[1] * light_intensity;
							color[2] += lights[l].color[2] * light_intensity;
						}
					}
				}
				if (color[0] > 1.0f)
					color[0] = 1.0f;
				if (color[1] > 1.0f)
					color[1] = 1.0f;
				if (color[2] > 1.0f)
					color[2] = 1.0f;
				vertex.color[0] = vertex.color[0] * color[0];
				vertex.color[1] = vertex.color[1] * color[1];
				vertex.color[2] = vertex.color[2] * color[2];
			}
		}
	}

	/************************* OpCode functions *************************/

	protected OpCode F3DEXBG_Vtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPVertexBg(segmentToPhysical(w1), (w0 >> 12) & 0xFF, ((w0 >> 1) & 0x7F));
		}
	};

	protected OpCode F3DEXBG_MoveWord = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			int index = (w0 >> 16) & 0xFF;
			int offset = w0 & 0xFFFF;
			int data = w1;

			switch (index)
			{
			case 0x02: // G_MW_NUMLIGHT:
				gSPNumLights(data / 48 /* 24 */);
				break;
			case 0x04: // G_MW_CLIP:
				break;
			case 0x06: // G_MW_SEGMENT:
				gSPSegment(offset >> 2, data & 0x00FFFFFF);
				break;
			case 0x08: // G_MW_FOG:
				gSPFogFactor((short) ((data >> 16) & 0xFFFF), (short) (data & 0xFFFF));
				break;
			case 0x0C: // G_MW_FORCEMTX:
				// Handled in movemem
				break;
			case 0x0E: // G_MW_PERSPNORM:
				break;
			case 0x10:
			{ // moveword coord mod
				if ((w0 & 8) != 0)
					return;
				gSPCoordMod(w1, (w0 >> 1) & 3, w0 & 0x30);
				break;
			}
			default:
				System.err.printf("moveword unknown (index: 0x%08X, offset 0x%08X)\n", index, offset);
			}
		}
	};

	protected OpCode F3DEXBG_MoveMem = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			int idx = w0 & 0xFF;
			switch (idx)
			{
			case 8: // F3DEX2_MV_VIEWPORT:
				int addr = segmentToPhysical(w1);
				if ((addr + 16) <= rdramSize)
					dp_writeRegister(DP_VIEWPORT_REG, addr);
				break;
			case 10: // G_MV_LIGHT:
				gSPLightBg(segmentToPhysical(w1), ((w0 >> 5) & 0x3FFF) / 48);
				break;
			case 14: // Normales
				gSPNormals(segmentToPhysical(w1));
				break;
			default:
				System.err.printf("movemem unknown (%d)\n", idx);
			}
		}
	};

	protected OpCode F3DEXBG_Tri4 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSP4Triangles(
					(w0 >> 23) & 0x1F, (w0 >> 18) & 0x1F, ((((w0 >> 15) & 0x7) << 2) | ((w1 >> 30) & 0x3)),
					(w0 >> 10) & 0x1F, (w0 >> 5) & 0x1F, (w0 >> 0) & 0x1F,
					(w1 >> 25) & 0x1F, (w1 >> 20) & 0x1F, (w1 >> 15) & 0x1F,
					(w1 >> 10) & 0x1F, (w1 >> 5) & 0x1F, (w1 >> 0) & 0x1F);
		}
	};
}
