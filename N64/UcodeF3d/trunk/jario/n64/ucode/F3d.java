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

import jario.hardware.Bus16bit;
import jario.hardware.Bus32bit;
import jario.hardware.Bus64bit;
import jario.hardware.Bus8bit;
import jario.hardware.BusDMA;
import jario.hardware.Clockable;
import jario.hardware.Hardware;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class F3d implements Hardware, Clockable, Bus32bit, Bus64bit
{
	protected static final int RDRAM_CAPACITY_REG = 10;

	protected static final int SP_DLPC_STACK_REG = 9;
	protected static final int SP_DLPC_REG = 10;
	protected static final int SP_DL_REG = 11;
	protected static final int SP_NON_REG = 12;

	protected static final int DP_OTHERMODE_LO_REG = 8;
	protected static final int DP_OTHERMODE_HI_REG = 9;
	protected static final int DP_VIEWPORT_REG = 10;
	protected static final int DP_CULL_FACE_REG = 12;
	protected static final int DP_FOG_REG = 13;
	protected static final int DP_DEPTH_REG = 14;

	protected static final int CHANGED_MATRIX = 0x02;
	protected static final int CHANGED_COLORBUFFER = 0x04;
	protected static final int CHANGED_GEOMETRYMODE = 0x08;
	protected static final int CHANGED_FOGPOSITION = 0x10;
	protected static final int CHANGED_VIEWPORT = 0x20;

	protected static final int PIPELINE_MODE = 0x800000;
	protected static final int CYCLE_TYPE = 0x300000;
	protected static final int TEX_PERSP = 0x80000;
	protected static final int TEX_DETAIL = 0x60000;
	protected static final int TEX_LOD = 0x10000;
	protected static final int TEX_LUT = 0xC000;
	protected static final int TEX_FILTER = 0x3000;
	protected static final int TEX_CONVERT = 0xE00;
	protected static final int COMBINE_KEY = 0x100;
	protected static final int COLOR_DITHER = 0xC0;
	protected static final int ALPHA_DITHER = 0x30;
	protected static final int DEPTH_SOURCE = 0x4;
	protected static final int ALPHA_COMPARE = 0x3;

	protected static final int F3D_MTX_STACKSIZE = 10;

	protected static final int F3D_MTX_MODELVIEW = 0x00;
	protected static final int F3D_MTX_PROJECTION = 0x01;
	protected static final int F3D_MTX_MUL = 0x00;
	protected static final int F3D_MTX_LOAD = 0x02;
	protected static final int F3D_MTX_NOPUSH = 0x00;
	protected static final int F3D_MTX_PUSH = 0x04;

	protected static final int F3D_TEXTURE_ENABLE = 0x00000002;
	protected static final int F3D_SHADING_SMOOTH = 0x00000200;
	protected static final int F3D_CULL_FRONT = 0x00001000;
	protected static final int F3D_CULL_BACK = 0x00002000;
	protected static final int F3D_CULL_BOTH = 0x00003000;
	protected static final int F3D_CLIPPING = 0x00000000;

	protected static final int F3D_MV_VIEWPORT = 0x80;

	protected static final int F3D_MWO_aLIGHT_1 = 0x00;
	protected static final int F3D_MWO_bLIGHT_1 = 0x04;
	protected static final int F3D_MWO_aLIGHT_2 = 0x20;
	protected static final int F3D_MWO_bLIGHT_2 = 0x24;
	protected static final int F3D_MWO_aLIGHT_3 = 0x40;
	protected static final int F3D_MWO_bLIGHT_3 = 0x44;
	protected static final int F3D_MWO_aLIGHT_4 = 0x60;
	protected static final int F3D_MWO_bLIGHT_4 = 0x64;
	protected static final int F3D_MWO_aLIGHT_5 = 0x80;
	protected static final int F3D_MWO_bLIGHT_5 = 0x84;
	protected static final int F3D_MWO_aLIGHT_6 = 0xa0;
	protected static final int F3D_MWO_bLIGHT_6 = 0xa4;
	protected static final int F3D_MWO_aLIGHT_7 = 0xc0;
	protected static final int F3D_MWO_bLIGHT_7 = 0xc4;
	protected static final int F3D_MWO_aLIGHT_8 = 0xe0;
	protected static final int F3D_MWO_bLIGHT_8 = 0xe4;

	// FAST3D commands
	protected static final int F3D_SPNOOP = 0x00;
	protected static final int F3D_MTX = 0x01;
	protected static final int F3D_RESERVED0 = 0x02;
	protected static final int F3D_MOVEMEM = 0x03;
	protected static final int F3D_VTX = 0x04;
	protected static final int F3D_RESERVED1 = 0x05;
	protected static final int F3D_DL = 0x06;
	protected static final int F3D_RESERVED2 = 0x07;
	protected static final int F3D_RESERVED3 = 0x08;
	protected static final int F3D_SPRITE2D_BASE = 0x09;

	protected static final int F3D_TRI1 = 0xBF;
	protected static final int F3D_CULLDL = 0xBE;
	protected static final int F3D_POPMTX = 0xBD;
	protected static final int F3D_MOVEWORD = 0xBC;
	protected static final int F3D_TEXTURE = 0xBB;
	protected static final int F3D_SETOTHERMODE_H = 0xBA;
	protected static final int F3D_SETOTHERMODE_L = 0xB9;
	protected static final int F3D_ENDDL = 0xB8;
	protected static final int F3D_SETGEOMETRYMODE = 0xB7;
	protected static final int F3D_CLEARGEOMETRYMODE = 0xB6;
	protected static final int F3D_QUAD = 0xB5;
	protected static final int F3D_RDPHALF_1 = 0xB4;
	protected static final int F3D_RDPHALF_2 = 0xB3;
	protected static final int F3D_RDPHALF_CONT = 0xB2;
	protected static final int F3D_TRI4 = 0xB1;

	public static interface OpCode
	{
		public void exec(int inst1, int inst2);
	}

	protected static class DMAOffsets
	{
		public int vtx;
		public int mtx;
	};

	protected static class SPVertex
	{
		public float[] vtx = new float[4];
		public float[] norm = new float[4];
		public float[] color = new float[4];
		public float[] tex = new float[2];
		public float[] clip = new float[3];
		public short flag;

		public void clip()
		{
			if (vtx[0] < -vtx[3])
			{
				clip[0] = -1.0f;
			}
			else if (vtx[0] > vtx[3])
			{
				clip[0] = 1.0f;
			}
			else
			{
				clip[0] = 0.0f;
			}

			if (vtx[1] < -vtx[3])
			{
				clip[1] = -1.0f;
			}
			else if (vtx[1] > vtx[3])
			{
				clip[1] = 1.0f;
			}
			else
			{
				clip[1] = 0.0f;
			}

			if (vtx[3] <= 0.0f)
			{
				clip[2] = -1.0f;
			}
			else if (vtx[2] < -vtx[3])
			{
				clip[2] = -0.1f;
			}
			else if (vtx[2] > vtx[3])
			{
				clip[2] = 1.0f;
			}
			else
			{
				clip[2] = 0.0f;
			}
		}
	};

	protected static class Matrix
	{
		public int modelViewi;
		public int stackSize;
		public int billboard;
		public float[][][] modelView = new float[32][4][4];
		public float[][] projection = new float[4][4];
		public float[][] combined = new float[4][4];
	};

	protected static class Light
	{
		public float[] color = new float[3];
		public float[] vec = new float[3];
		// for microcode F3DEXBG
		public float x;
		public float y;
		public float z;
		public float w;
		public int nonzero;
		public int nonblack;
		public float a;
		public float ca;
	};

	protected static class Fog
	{
		public short multiplier;
		public short offset;
	};

	protected static OpCode gbiUnknown = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected static int half_1, half_2;

	protected static Bus32bit sp;
	protected static Bus32bit dp;
	protected static BusDMA dpDMA;
	protected static Bus8bit rdram;
	protected static Bus16bit rdram16bit;
	protected static Hardware dmem;
	protected static int rdramSize;
	protected static boolean init;
	protected static ByteBuffer cmdBuffer = ByteBuffer.allocate(128);
	protected static byte[] cmdBuf = cmdBuffer.array();

	protected static int[] segment = new int[16];
	public static int geometryMode;
	public static int changed;

	// vertex buffer should be in dmem
	protected static SPVertex[] vertices = new SPVertex[80];
	protected static int vertexi;
	protected static Matrix matrix = new Matrix();
	protected static DMAOffsets DMAOffsets = new DMAOffsets();
	protected static float[][] tmpmtx = new float[4][4];
	protected static int numLights;
	protected static int uc8_normale_addr = 0;
	protected static float[] uc8_coord_mod = new float[16];
	protected static Light[] lights = new Light[12];
	protected static Fog fog = new Fog();
	protected static int nextCmd;
	protected static ByteBuffer dpcmdbuf = ByteBuffer.allocate(256);
	protected static byte[] dpcmd = dpcmdbuf.array();

	protected static OpCode[] dlist = new OpCode[256];

	protected static final int dp_readRegister(int address)
	{
		return dp.read32bit(0x04100000 + (address << 2));
	}

	protected static final void dp_writeRegister(int address, int value)
	{
		dp.write32bit(0x04100000 + (address << 2), value);
	}

	protected static final int sp_readRegister(int address)
	{
		return sp.read32bit(0x04040000 + (address << 2));
	}

	protected static final void sp_writeRegister(int address, int value)
	{
		sp.write32bit(0x04040000 + (address << 2), value);
	}

	public F3d()
	{
		if (!init)
		{
			for (int i = 0; i < vertices.length; i++)
			{
				vertices[i] = new SPVertex();
			}
			for (int i = 0; i < lights.length; i++)
			{
				lights[i] = new Light();
			}
			changed = 0xFFFFFFFF;

			for (int i = 0; i < dlist.length; i++)
				dlist[i] = gbiUnknown;

			init = true;
		}
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0: // not used
			break;
		case 1:
			rdram = (Bus8bit) bus;
			rdram16bit = (Bus16bit) bus;
			if (rdram != null)
				rdramSize = ((Bus32bit) rdram).read32bit(0x03F00028);
			break;
		case 2:
			sp = (Bus32bit) bus;
			break;
		case 3:
			dp = (Bus32bit) bus;
			dpDMA = (BusDMA) bus;
			break;
		}
	}

	@Override
	public void reset()
	{
		changed = 0xFFFFFFFF;
	}

	@Override
	public void clock(long ticks)
	{
		gbiInitFlags();

		Gbi.G_TRI1 = F3D_TRI1;
		Gbi.G_QUAD = F3D_QUAD;
		Gbi.G_TRI4 = F3D_TRI4;

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
	}

	@Override
	public int read32bit(int reg)
	{
		return 0;
	}
	
	@Override
	public void write32bit(int reg, int value)
	{
		switch (reg)
		{
		case 0:
			initMatrix(StrictMath.min(32, value >> 6));
			changed |= CHANGED_MATRIX;
			break;
		}
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
		OpCode op = dlist[inst1 >>> 24];
		op.exec(inst1, inst2);
	}

	// Protected Methods ///////////////////////////////////////////////////////

	protected static void gbiInitFlags()
	{
		Gbi.G_MTX_PROJECTION = F3D_MTX_PROJECTION;
		Gbi.G_MTX_LOAD = F3D_MTX_LOAD;
		Gbi.G_MTX_PUSH = F3D_MTX_PUSH;
		Gbi.G_CULL_BACK = F3D_CULL_BACK;
		Gbi.G_CULL_BOTH = F3D_CULL_BOTH;
	}

	// Matrix //////////////////////////////////////////////////////////////////

	protected static void initMatrix(int stackSize)
	{
		matrix.stackSize = stackSize;
		matrix.modelViewi = 0;

		for (int i = 0; i < 4; i++)
			Arrays.fill(matrix.modelView[0][i], 0.0f);

		matrix.modelView[0][0][0] = 1.0f;
		matrix.modelView[0][1][1] = 1.0f;
		matrix.modelView[0][2][2] = 1.0f;
		matrix.modelView[0][3][3] = 1.0f;
	}

	protected static void loadMatrix(float[][] mtx, int address)
	{
		final float recip = 1.5258789e-05f;
		for (int i = 0; i < 4; i++)
		{
			mtx[i][0] = rdram16bit.read16bit(address) + (rdram16bit.read16bit(address + 32) & 0xFFFF) * recip;
			mtx[i][1] = rdram16bit.read16bit(address + 2) + (rdram16bit.read16bit(address + 34) & 0xFFFF) * recip;
			mtx[i][2] = rdram16bit.read16bit(address + 4) + (rdram16bit.read16bit(address + 36) & 0xFFFF) * recip;
			mtx[i][3] = rdram16bit.read16bit(address + 6) + (rdram16bit.read16bit(address + 38) & 0xFFFF) * recip;
			address += 8;
		}
	}

	protected static void gSPMatrix(int address, int param)
	{
		if (address + 64 > rdramSize)
			return;
		loadMatrix(tmpmtx, address);
		if ((param & Gbi.G_MTX_PROJECTION) != 0)
		{
			if ((param & Gbi.G_MTX_LOAD) != 0)
				Math3D.copyMatrix(matrix.projection, tmpmtx);
			else
				Math3D.multMatrix(matrix.projection, tmpmtx);
		}
		else
		{
			if ((param & Gbi.G_MTX_PUSH) != 0 && (matrix.modelViewi < (matrix.stackSize - 1)))
			{
				Math3D.copyMatrix(matrix.modelView[matrix.modelViewi + 1], matrix.modelView[matrix.modelViewi]);
				matrix.modelViewi++;
			}
			if ((param & Gbi.G_MTX_LOAD) != 0)
				Math3D.copyMatrix(matrix.modelView[matrix.modelViewi], tmpmtx);
			else
				Math3D.multMatrix(matrix.modelView[matrix.modelViewi], tmpmtx);
		}
		changed |= CHANGED_MATRIX;
	}

	protected static void gSPForceMatrix(int address)
	{
		if (address + 64 > rdramSize)
			return;
		loadMatrix(matrix.combined, address);
		changed &= ~CHANGED_MATRIX;
	}

	protected static void gSPInsertMatrix(int where, int num)
	{
		float fraction, integer;

		if ((changed & CHANGED_MATRIX) != 0)
			gSPCombineMatrices();

		if ((where & 0x3) != 0 || (where > 0x3C))
			return;

		if (where < 0x20)
		{
			int w = where >> 1;
			int r = w / 4;
			int c = w % 4;
			int w2 = w + 1;
			int r2 = w2 / 4;
			int c2 = w2 % 4;
			integer = (int) matrix.combined[r][c];
			fraction = matrix.combined[r][c] - integer;
			matrix.combined[r][c] = (short) ((num >> 16) & Gbi.SR_MASK_16) + StrictMath.abs(fraction);

			integer = (int) matrix.combined[r2][c2];
			fraction = matrix.combined[r2][c2] - integer;
			matrix.combined[r2][c2] = (short) (num & Gbi.SR_MASK_16) + StrictMath.abs(fraction);
		}
		else
		{
			int w = (where - 0x20) >> 1;
			int r = w / 4;
			int c = w % 4;
			int w2 = w + 1;
			int r2 = w2 / 4;
			int c2 = w2 % 4;
			float newValue;

			integer = (int) matrix.combined[r][c];
			fraction = matrix.combined[r][c] - integer;
			newValue = integer + (((num >> 16) & Gbi.SR_MASK_16) * Gbi.FIXED2FLOATRECIP16);

			// Make sure the sign isn't lost
			if ((integer == 0.0f) && (fraction != 0.0f))
				newValue = newValue * (fraction / StrictMath.abs(fraction));

			matrix.combined[r][c] = newValue;

			integer = (int) matrix.combined[r2][c2];
			fraction = matrix.combined[r2][c2] - integer;
			newValue = integer + ((num & Gbi.SR_MASK_16) * Gbi.FIXED2FLOATRECIP16);

			// Make sure the sign isn't lost
			if ((integer == 0.0f) && (fraction != 0.0f))
				newValue = newValue * (fraction / StrictMath.abs(fraction));

			matrix.combined[r2][c2] = newValue;
		}
	}

	protected static void gSPPopMatrix(int param)
	{
		if (matrix.modelViewi > 0)
		{
			matrix.modelViewi--;
			changed |= CHANGED_MATRIX;
		}
	}

	protected static void gSPCombineMatrices()
	{
		Math3D.copyMatrix(matrix.combined, matrix.projection);
		Math3D.multMatrix(matrix.combined, matrix.modelView[matrix.modelViewi]);
		changed &= ~CHANGED_MATRIX;
	}

	// Vertex //////////////////////////////////////////////////////////////////

	protected static void gSPVertex(int address, int n, int v0)
	{
		if ((address + 16 * n) > rdramSize)
			return;
		if ((n + v0) < (80))
		{
			SPVertex vertex;
			for (int i = v0; i < n + v0; i++)
			{
				vertex = vertices[i];
				vertex.vtx[0] = rdram16bit.read16bit(address);
				vertex.vtx[1] = rdram16bit.read16bit(address + 2);
				vertex.vtx[2] = rdram16bit.read16bit(address + 4);

				vertex.flag = rdram16bit.read16bit(address + 6);
				vertex.tex[0] = rdram16bit.read16bit(address + 8) * Gbi.FIXED2FLOATRECIP5;
				vertex.tex[1] = rdram16bit.read16bit(address + 10) * Gbi.FIXED2FLOATRECIP5;

				if ((geometryMode & Gbi.G_LIGHTING) != 0)
				{
					vertex.norm[0] = rdram.read8bit(address + 12);
					vertex.norm[1] = rdram.read8bit(address + 13);
					vertex.norm[2] = rdram.read8bit(address + 14);
					vertex.color[3] = (rdram.read8bit(address + 15) & 0xFF) * 0.0039215689f;
				}
				else
				{
					vertex.color[0] = (rdram.read8bit(address + 12) & 0xFF) * 0.0039215689f;
					vertex.color[1] = (rdram.read8bit(address + 13) & 0xFF) * 0.0039215689f;
					vertex.color[2] = (rdram.read8bit(address + 14) & 0xFF) * 0.0039215689f;
					vertex.color[3] = (rdram.read8bit(address + 15) & 0xFF) * 0.0039215689f;
				}

				gSPProcessVertex(vertex);
				address += 16;
			}
		}
	}

	protected static void gSPModifyVertex(int vtx, int where, int val)
	{
		switch (where)
		{
		case Gbi.G_MWO_POINT_RGBA:
			vertices[vtx].color[0] = ((val >> 24) & Gbi.SR_MASK_8) * 0.0039215689f;
			vertices[vtx].color[1] = ((val >> 16) & Gbi.SR_MASK_8) * 0.0039215689f;
			vertices[vtx].color[2] = ((val >> 8) & Gbi.SR_MASK_8) * 0.0039215689f;
			vertices[vtx].color[3] = (val & Gbi.SR_MASK_8) * 0.0039215689f;
			break;
		case Gbi.G_MWO_POINT_ST:
			vertices[vtx].tex[0] = ((short) ((val >> 16) & Gbi.SR_MASK_16)) * Gbi.FIXED2FLOATRECIP5;
			vertices[vtx].tex[1] = ((short) (val & Gbi.SR_MASK_16)) * Gbi.FIXED2FLOATRECIP5;
			break;
		case Gbi.G_MWO_POINT_XYSCREEN:
			break;
		case Gbi.G_MWO_POINT_ZSCREEN:
			break;
		}
	}

	protected static void gSPProcessVertex(SPVertex vert)
	{
		float intensity;
		float r, g, b;

		if ((changed & CHANGED_MATRIX) != 0)
			gSPCombineMatrices();

		Math3D.transformVertex(vert.vtx, matrix.combined);

		if (matrix.billboard != 0)
		{
			vert.vtx[0] += vertices[0].vtx[0];
			vert.vtx[1] += vertices[0].vtx[1];
			vert.vtx[2] += vertices[0].vtx[2];
			vert.vtx[3] += vertices[0].vtx[3];
		}

		if ((geometryMode & Gbi.G_ZBUFFER) == 0)
		{
			vert.vtx[2] = -vert.vtx[3];
		}

		if ((geometryMode & Gbi.G_LIGHTING) != 0)
		{
			Math3D.transformVector(vert.norm, matrix.modelView[matrix.modelViewi]);
			Math3D.normalize(vert.norm);

			r = lights[numLights].color[0];
			g = lights[numLights].color[1];
			b = lights[numLights].color[2];

			for (int i = 0; i < numLights; i++)
			{
				intensity = Math3D.dotProduct(vert.norm, lights[i].vec);

				if (intensity < 0.0f)
				{
					intensity = 0.0f;
				}

				r += lights[i].color[0] * intensity;
				g += lights[i].color[1] * intensity;
				b += lights[i].color[2] * intensity;
			}

			vert.color[0] = r;
			vert.color[1] = g;
			vert.color[2] = b;

			if ((geometryMode & Gbi.G_TEXTURE_GEN) != 0)
			{
				Math3D.transformVector(vert.norm, matrix.projection);

				Math3D.normalize(vert.norm);

				if ((geometryMode & Gbi.G_TEXTURE_GEN_LINEAR) != 0)
				{
					vert.tex[0] = (float) StrictMath.acos(vert.norm[0]) * 325.94931f;
					vert.tex[1] = (float) StrictMath.acos(vert.norm[1]) * 325.94931f;
				}
				else
				{ // G_TEXTURE_GEN
					vert.tex[0] = (vert.norm[0] + 1.0f) * 512.0f;
					vert.tex[1] = (vert.norm[1] + 1.0f) * 512.0f;
				}
			}
		}

		vert.clip();
	}

	protected static boolean gSPCullVertices(int v0, int vn)
	{
		float xClip, yClip, zClip;

		xClip = yClip = zClip = 0.0f;

		for (int i = v0; i <= vn; i++)
		{
			if (vertices[i].clip[0] == 0.0f)
				return false;
			else if (vertices[i].clip[0] < 0.0f)
			{
				if (xClip > 0.0f)
					return false;
				else
					xClip = vertices[i].clip[0];
			}
			else if (vertices[i].clip[0] > 0.0f)
			{
				if (xClip < 0.0f)
					return false;
				else
					xClip = vertices[i].clip[0];
			}

			if (vertices[i].clip[1] == 0.0f)
				return false;
			else if (vertices[i].clip[1] < 0.0f)
			{
				if (yClip > 0.0f)
					return false;
				else
					yClip = vertices[i].clip[1];
			}
			else if (vertices[i].clip[1] > 0.0f)
			{
				if (yClip < 0.0f)
					return false;
				else
					yClip = vertices[i].clip[1];
			}

			if (vertices[i].clip[2] == 0.0f)
				return false;
			else if (vertices[i].clip[2] < 0.0f)
			{
				if (zClip > 0.0f)
					return false;
				else
					zClip = vertices[i].clip[2];
			}
			else if (vertices[i].clip[2] > 0.0f)
			{
				if (zClip < 0.0f)
					return false;
				else
					zClip = vertices[i].clip[2];
			}
		}

		return true;
	}

	public static void gSPCullDisplayList(int v0, int vn)
	{
		if (gSPCullVertices(v0, vn))
		{
			sp_readRegister(SP_DL_REG);
		}
	}

	// Triangle ////////////////////////////////////////////////////////////////

	protected static void gSPTriangle(int v0, int v1, int v2, int flag)
	{
		if ((v0 < 80) && (v1 < 80) && (v2 < 80))
		{
			// Don't bother with triangles completely outside clipping frustrum
			if (((vertices[v0].clip[0] < 0.0f) &&
					(vertices[v1].clip[0] < 0.0f) &&
					(vertices[v2].clip[0] < 0.0f)) ||
					((vertices[v0].clip[0] > 0.0f) &&
							(vertices[v1].clip[0] > 0.0f) &&
					(vertices[v2].clip[0] > 0.0f)) ||
					((vertices[v0].clip[1] < 0.0f) &&
							(vertices[v1].clip[1] < 0.0f) &&
					(vertices[v2].clip[1] < 0.0f)) ||
					((vertices[v0].clip[1] > 0.0f) &&
							(vertices[v1].clip[1] > 0.0f) &&
					(vertices[v2].clip[1] > 0.0f)) ||
					((vertices[v0].clip[2] > 0.1f) &&
							(vertices[v1].clip[2] > 0.1f) &&
					(vertices[v2].clip[2] > 0.1f)) ||
					((vertices[v0].clip[2] < -0.1f) &&
							(vertices[v1].clip[2] < -0.1f) &&
					(vertices[v2].clip[2] < -0.1f)))
				return;

			updateStates();

			createTriangleCommand(vertices[v0].vtx, vertices[v0].color, vertices[v0].tex, vertices[v0].clip,
					vertices[v1].vtx, vertices[v1].color, vertices[v1].tex, vertices[v1].clip,
					vertices[v2].vtx, vertices[v2].color, vertices[v2].tex, vertices[v2].clip);
			((BusDMA) dp).writeDMA(0x10, dpcmdbuf, 0, 176);
		}
	}

	protected static void gSPFlushTriangles()
	{
		nextCmd = (((Bus32bit) rdram).read32bit(sp_readRegister(SP_DLPC_REG)) >> 24) & Gbi.SR_MASK_8;
		if ((nextCmd != Gbi.G_TRI1) &&
				(nextCmd != Gbi.G_TRI2) &&
				(nextCmd != Gbi.G_TRI4) &&
				(nextCmd != Gbi.G_QUAD) &&
				(nextCmd != Gbi.G_DMA_TRI))
			dp_readRegister(SP_DL_REG);
	}

	protected static void gSP1Triangle(int v0, int v1, int v2, int flag)
	{
		gSPTriangle(v0, v1, v2, flag);
		gSPFlushTriangles();
	}

	protected static void gSP4Triangles(
			int v00, int v01, int v02,
			int v10, int v11, int v12,
			int v20, int v21, int v22,
			int v30, int v31, int v32)
	{
		gSPTriangle(v00, v01, v02, 0);
		gSPTriangle(v10, v11, v12, 0);
		gSPTriangle(v20, v21, v22, 0);
		gSPTriangle(v30, v31, v32, 0);
		gSPFlushTriangles();
	}

	protected static void gSP1Quadrangle(int v0, int v1, int v2, int v3)
	{
		gSPTriangle(v0, v1, v2, 0);
		gSPTriangle(v0, v2, v3, 0);
		gSPFlushTriangles();
	}

	protected static void createTriangleCommand(float[] vtx1, float[] c1, float[] tex1, float[] clip1, float[] vtx2, float[] c2, float[] tex2, float[] clip2, float[] vtx3, float[] c3, float[] tex3, float[] clip3)
	{
		dpcmd[0] = 0x10;
		dpcmd[1] = (byte) sp_readRegister(SP_NON_REG);

		// Edge
		int vtx1_x = Float.floatToIntBits(vtx1[0]);
		dpcmd[8] = (byte) (vtx1_x >> 24);
		dpcmd[9] = (byte) (vtx1_x >> 16);
		dpcmd[10] = (byte) (vtx1_x >> 8);
		dpcmd[11] = (byte) (vtx1_x);
		int vtx1_y = Float.floatToIntBits(vtx1[1]);
		dpcmd[12] = (byte) (vtx1_y >> 24);
		dpcmd[13] = (byte) (vtx1_y >> 16);
		dpcmd[14] = (byte) (vtx1_y >> 8);
		dpcmd[15] = (byte) (vtx1_y);
		int vtx1_z = Float.floatToIntBits(vtx1[2]);
		dpcmd[16] = (byte) (vtx1_z >> 24);
		dpcmd[17] = (byte) (vtx1_z >> 16);
		dpcmd[18] = (byte) (vtx1_z >> 8);
		dpcmd[19] = (byte) (vtx1_z);
		int vtx1_w = Float.floatToIntBits(vtx1[3]);
		dpcmd[20] = (byte) (vtx1_w >> 24);
		dpcmd[21] = (byte) (vtx1_w >> 16);
		dpcmd[22] = (byte) (vtx1_w >> 8);
		dpcmd[23] = (byte) (vtx1_w);
		int vtx2_x = Float.floatToIntBits(vtx2[0]);
		dpcmd[24] = (byte) (vtx2_x >> 24);
		dpcmd[25] = (byte) (vtx2_x >> 16);
		dpcmd[26] = (byte) (vtx2_x >> 8);
		dpcmd[27] = (byte) (vtx2_x);
		int vtx2_y = Float.floatToIntBits(vtx2[1]);
		dpcmd[28] = (byte) (vtx2_y >> 24);
		dpcmd[29] = (byte) (vtx2_y >> 16);
		dpcmd[30] = (byte) (vtx2_y >> 8);
		dpcmd[31] = (byte) (vtx2_y);
		int vtx2_z = Float.floatToIntBits(vtx2[2]);
		dpcmd[32] = (byte) (vtx2_z >> 24);
		dpcmd[33] = (byte) (vtx2_z >> 16);
		dpcmd[34] = (byte) (vtx2_z >> 8);
		dpcmd[35] = (byte) (vtx2_z);
		int vtx2_w = Float.floatToIntBits(vtx2[3]);
		dpcmd[36] = (byte) (vtx2_w >> 24);
		dpcmd[37] = (byte) (vtx2_w >> 16);
		dpcmd[38] = (byte) (vtx2_w >> 8);
		dpcmd[39] = (byte) (vtx2_w);
		int vtx3_x = Float.floatToIntBits(vtx3[0]);
		dpcmd[40] = (byte) (vtx3_x >> 24);
		dpcmd[41] = (byte) (vtx3_x >> 16);
		dpcmd[42] = (byte) (vtx3_x >> 8);
		dpcmd[43] = (byte) (vtx3_x);
		int vtx3_y = Float.floatToIntBits(vtx3[1]);
		dpcmd[44] = (byte) (vtx3_y >> 24);
		dpcmd[45] = (byte) (vtx3_y >> 16);
		dpcmd[46] = (byte) (vtx3_y >> 8);
		dpcmd[47] = (byte) (vtx3_y);
		int vtx3_z = Float.floatToIntBits(vtx3[2]);
		dpcmd[48] = (byte) (vtx3_z >> 24);
		dpcmd[49] = (byte) (vtx3_z >> 16);
		dpcmd[50] = (byte) (vtx3_z >> 8);
		dpcmd[51] = (byte) (vtx3_z);
		int vtx3_w = Float.floatToIntBits(vtx3[3]);
		dpcmd[52] = (byte) (vtx3_w >> 24);
		dpcmd[53] = (byte) (vtx3_w >> 16);
		dpcmd[54] = (byte) (vtx3_w >> 8);
		dpcmd[55] = (byte) (vtx3_w);

		// Shade
		int c1_r = Float.floatToIntBits(c1[0]);
		dpcmd[56] = (byte) (c1_r >> 24);
		dpcmd[57] = (byte) (c1_r >> 16);
		dpcmd[58] = (byte) (c1_r >> 8);
		dpcmd[59] = (byte) (c1_r);
		int c1_g = Float.floatToIntBits(c1[1]);
		dpcmd[60] = (byte) (c1_g >> 24);
		dpcmd[61] = (byte) (c1_g >> 16);
		dpcmd[62] = (byte) (c1_g >> 8);
		dpcmd[63] = (byte) (c1_g);
		int c1_b = Float.floatToIntBits(c1[2]);
		dpcmd[64] = (byte) (c1_b >> 24);
		dpcmd[65] = (byte) (c1_b >> 16);
		dpcmd[66] = (byte) (c1_b >> 8);
		dpcmd[67] = (byte) (c1_b);
		int c1_a = Float.floatToIntBits(c1[3]);
		dpcmd[68] = (byte) (c1_a >> 24);
		dpcmd[69] = (byte) (c1_a >> 16);
		dpcmd[70] = (byte) (c1_a >> 8);
		dpcmd[71] = (byte) (c1_a);
		int c2_r = Float.floatToIntBits(c2[0]);
		dpcmd[72] = (byte) (c2_r >> 24);
		dpcmd[73] = (byte) (c2_r >> 16);
		dpcmd[74] = (byte) (c2_r >> 8);
		dpcmd[75] = (byte) (c2_r);
		int c2_g = Float.floatToIntBits(c2[1]);
		dpcmd[76] = (byte) (c2_g >> 24);
		dpcmd[77] = (byte) (c2_g >> 16);
		dpcmd[78] = (byte) (c2_g >> 8);
		dpcmd[79] = (byte) (c2_g);
		int c2_b = Float.floatToIntBits(c2[2]);
		dpcmd[80] = (byte) (c2_b >> 24);
		dpcmd[81] = (byte) (c2_b >> 16);
		dpcmd[82] = (byte) (c2_b >> 8);
		dpcmd[83] = (byte) (c2_b);
		int c2_a = Float.floatToIntBits(c2[3]);
		dpcmd[84] = (byte) (c2_a >> 24);
		dpcmd[85] = (byte) (c2_a >> 16);
		dpcmd[86] = (byte) (c2_a >> 8);
		dpcmd[87] = (byte) (c2_a);
		int c3_r = Float.floatToIntBits(c3[0]);
		dpcmd[88] = (byte) (c3_r >> 24);
		dpcmd[89] = (byte) (c3_r >> 16);
		dpcmd[90] = (byte) (c3_r >> 8);
		dpcmd[91] = (byte) (c3_r);
		int c3_g = Float.floatToIntBits(c3[1]);
		dpcmd[92] = (byte) (c3_g >> 24);
		dpcmd[93] = (byte) (c3_g >> 16);
		dpcmd[94] = (byte) (c3_g >> 8);
		dpcmd[95] = (byte) (c3_g);
		int c3_b = Float.floatToIntBits(c3[2]);
		dpcmd[96] = (byte) (c3_b >> 24);
		dpcmd[97] = (byte) (c3_b >> 16);
		dpcmd[98] = (byte) (c3_b >> 8);
		dpcmd[99] = (byte) (c3_b);
		int c3_a = Float.floatToIntBits(c3[3]);
		dpcmd[100] = (byte) (c3_a >> 24);
		dpcmd[101] = (byte) (c3_a >> 16);
		dpcmd[102] = (byte) (c3_a >> 8);
		dpcmd[103] = (byte) (c3_a);

		// Texture
		int tex1_s = Float.floatToIntBits(tex1[0]);
		dpcmd[104] = (byte) (tex1_s >> 24);
		dpcmd[105] = (byte) (tex1_s >> 16);
		dpcmd[106] = (byte) (tex1_s >> 8);
		dpcmd[107] = (byte) (tex1_s);
		int tex1_t = Float.floatToIntBits(tex1[1]);
		dpcmd[108] = (byte) (tex1_t >> 24);
		dpcmd[109] = (byte) (tex1_t >> 16);
		dpcmd[110] = (byte) (tex1_t >> 8);
		dpcmd[111] = (byte) (tex1_t);
		int tex2_s = Float.floatToIntBits(tex2[0]);
		dpcmd[112] = (byte) (tex2_s >> 24);
		dpcmd[113] = (byte) (tex2_s >> 16);
		dpcmd[114] = (byte) (tex2_s >> 8);
		dpcmd[115] = (byte) (tex2_s);
		int tex2_t = Float.floatToIntBits(tex2[1]);
		dpcmd[116] = (byte) (tex2_t >> 24);
		dpcmd[117] = (byte) (tex2_t >> 16);
		dpcmd[118] = (byte) (tex2_t >> 8);
		dpcmd[119] = (byte) (tex2_t);
		int tex3_s = Float.floatToIntBits(tex3[0]);
		dpcmd[120] = (byte) (tex3_s >> 24);
		dpcmd[121] = (byte) (tex3_s >> 16);
		dpcmd[122] = (byte) (tex3_s >> 8);
		dpcmd[123] = (byte) (tex3_s);
		int tex3_t = Float.floatToIntBits(tex3[1]);
		dpcmd[124] = (byte) (tex3_t >> 24);
		dpcmd[125] = (byte) (tex3_t >> 16);
		dpcmd[126] = (byte) (tex3_t >> 8);
		dpcmd[127] = (byte) (tex3_t);

		// Zbuffer
		int clip1_x = Float.floatToIntBits(clip1[0]);
		dpcmd[128] = (byte) (clip1_x >> 24);
		dpcmd[129] = (byte) (clip1_x >> 16);
		dpcmd[130] = (byte) (clip1_x >> 8);
		dpcmd[131] = (byte) (clip1_x);
		int clip1_y = Float.floatToIntBits(clip1[1]);
		dpcmd[132] = (byte) (clip1_y >> 24);
		dpcmd[133] = (byte) (clip1_y >> 16);
		dpcmd[134] = (byte) (clip1_y >> 8);
		dpcmd[135] = (byte) (clip1_y);
		int clip1_z = Float.floatToIntBits(clip1[2]);
		dpcmd[136] = (byte) (clip1_z >> 24);
		dpcmd[137] = (byte) (clip1_z >> 16);
		dpcmd[138] = (byte) (clip1_z >> 8);
		dpcmd[139] = (byte) (clip1_z);
		int clip2_x = Float.floatToIntBits(clip2[0]);
		dpcmd[144] = (byte) (clip2_x >> 24);
		dpcmd[145] = (byte) (clip2_x >> 16);
		dpcmd[146] = (byte) (clip2_x >> 8);
		dpcmd[147] = (byte) (clip2_x);
		int clip2_y = Float.floatToIntBits(clip2[1]);
		dpcmd[148] = (byte) (clip2_y >> 24);
		dpcmd[149] = (byte) (clip2_y >> 16);
		dpcmd[150] = (byte) (clip2_y >> 8);
		dpcmd[151] = (byte) (clip2_y);
		int clip2_z = Float.floatToIntBits(clip2[2]);
		dpcmd[152] = (byte) (clip2_z >> 24);
		dpcmd[153] = (byte) (clip2_z >> 16);
		dpcmd[154] = (byte) (clip2_z >> 8);
		dpcmd[155] = (byte) (clip2_z);
		int clip3_x = Float.floatToIntBits(clip3[0]);
		dpcmd[160] = (byte) (clip3_x >> 24);
		dpcmd[161] = (byte) (clip3_x >> 16);
		dpcmd[162] = (byte) (clip3_x >> 8);
		dpcmd[163] = (byte) (clip3_x);
		int clip3_y = Float.floatToIntBits(clip3[1]);
		dpcmd[164] = (byte) (clip3_y >> 24);
		dpcmd[165] = (byte) (clip3_y >> 16);
		dpcmd[166] = (byte) (clip3_y >> 8);
		dpcmd[167] = (byte) (clip3_y);
		int clip3_z = Float.floatToIntBits(clip3[2]);
		dpcmd[168] = (byte) (clip3_z >> 24);
		dpcmd[169] = (byte) (clip3_z >> 16);
		dpcmd[170] = (byte) (clip3_z >> 8);
		dpcmd[171] = (byte) (clip3_z);
	}

	// Shade ///////////////////////////////////////////////////////////////////

	protected static void gSPLight(int address, int n)
	{
		if ((address + 12) > rdramSize)
			return;
		n--;
		if (n < 12)
		{
			lights[n].color[0] = (rdram.read8bit(address) & 0xFF) * 0.0039215689f;
			lights[n].color[1] = (rdram.read8bit(address + 1) & 0xFF) * 0.0039215689f;
			lights[n].color[2] = (rdram.read8bit(address + 2) & 0xFF) * 0.0039215689f;
			lights[n].vec[0] = rdram.read8bit(address + 8);
			lights[n].vec[1] = rdram.read8bit(address + 9);
			lights[n].vec[2] = rdram.read8bit(address + 10);
			Math3D.normalize(lights[n].vec);
		}
	}

	protected static void gSPNumLights(int n)
	{
		if (n <= 8)
			numLights = n;
	}

	protected static void gSPLightColor(int lightNum, int packedColor)
	{
		lightNum--;
		if (lightNum < 12)
		{
			lights[lightNum].color[0] = ((packedColor >> 24) & Gbi.SR_MASK_8) * 0.0039215689f;
			lights[lightNum].color[1] = ((packedColor >> 16) & Gbi.SR_MASK_8) * 0.0039215689f;
			lights[lightNum].color[2] = ((packedColor >> 8) & Gbi.SR_MASK_8) * 0.0039215689f;
		}
	}

	protected static void gSPFogFactor(short fm, short fo)
	{
		// fog.multiplier = fm;
		// fog.offset = fo;
		// changed |= CHANGED_FOGPOSITION;
	}

	// Control /////////////////////////////////////////////////////////////////

	protected static int segmentToPhysical(int segaddr)
	{
		return (segment[(segaddr >> 24) & 0x0F] + (segaddr & 0x00FFFFFF)) & 0x00FFFFFF;
	}

	protected static void gSPSegment(int seg, int base)
	{
		if (seg > 0xF)
			return;
		if (base > rdramSize - 1)
			return;
		segment[seg] = base;
	}

	protected void gSPBranchList(int address)
	{
		if ((address + 8) > rdramSize)
			return;
		sp_writeRegister(SP_DLPC_REG, address);
	}

	protected void gSPSetGeometryMode(int mode)
	{
		geometryMode |= mode;
		changed |= CHANGED_GEOMETRYMODE;
	}

	protected void gSPClearGeometryMode(int mode)
	{
		geometryMode &= ~mode;
		changed |= CHANGED_GEOMETRYMODE;
	}

	protected void gSPGeometryMode(int clear, int set)
	{
		geometryMode = (geometryMode & ~clear) | set;
		changed |= CHANGED_GEOMETRYMODE;
	}

	protected static void updateStates()
	{
		if (changed == 0)
			return;

		if ((changed & CHANGED_GEOMETRYMODE) != 0)
		{
			if ((geometryMode & Gbi.G_CULL_BOTH) != 0)
			{
				if ((geometryMode & Gbi.G_CULL_BACK) != 0)
				{
					dp_writeRegister(DP_CULL_FACE_REG, 1);
				}
				else
				{
					dp_writeRegister(DP_CULL_FACE_REG, 2);
				}
			}
			else
			{
				dp_writeRegister(DP_CULL_FACE_REG, 0);
			}
			if ((geometryMode & Gbi.G_FOG) != 0)
			{
				dp_writeRegister(DP_FOG_REG, 1);
			}
			else
			{
				dp_writeRegister(DP_FOG_REG, 0);
			}
			if ((geometryMode & Gbi.G_ZBUFFER) != 0)
			{
				dp_writeRegister(DP_DEPTH_REG, 1);
			}
			else
			{
				dp_writeRegister(DP_DEPTH_REG, 0);
			}
			changed &= ~CHANGED_GEOMETRYMODE;
		}
		changed &= CHANGED_MATRIX;
	}

	protected int getWord()
	{
		int pc = sp_readRegister(SP_DLPC_REG);
		int w = ((Bus32bit) rdram).read32bit(pc);
		sp_writeRegister(SP_DLPC_REG, pc + 4);
		return w;
	}

	/************************* OpCode functions *************************/

	protected OpCode F3D_SPNoOp = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3D_Mtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			if ((w0 & Gbi.SR_MASK_16) != 64) {
				return;
			}
			gSPMatrix(segmentToPhysical(w1), (w0 >> 16) & Gbi.SR_MASK_8);
		}
	};

	protected OpCode F3D_Reserved0 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3D_MoveMem = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			w1 = segmentToPhysical(w1);
			switch ((w0 >> 16) & Gbi.SR_MASK_8)
			{
			case F3D_MV_VIEWPORT:// G_MV_VIEWPORT:
				if ((w1 + 16) <= rdramSize)
					dp_writeRegister(DP_VIEWPORT_REG, w1);
				break;
			case Gbi.G_MV_MATRIX_1:
				gSPForceMatrix(w1);
				// force matrix takes four commands
				getWord();
				getWord();
				getWord();
				getWord();
				getWord();
				getWord();
				break;
			case Gbi.G_MV_L0:
				gSPLight(w1, Gbi.LIGHT_1);
				break;
			case Gbi.G_MV_L1:
				gSPLight(w1, Gbi.LIGHT_2);
				break;
			case Gbi.G_MV_L2:
				gSPLight(w1, Gbi.LIGHT_3);
				break;
			case Gbi.G_MV_L3:
				gSPLight(w1, Gbi.LIGHT_4);
				break;
			case Gbi.G_MV_L4:
				gSPLight(w1, Gbi.LIGHT_5);
				break;
			case Gbi.G_MV_L5:
				gSPLight(w1, Gbi.LIGHT_6);
				break;
			case Gbi.G_MV_L6:
				gSPLight(w1, Gbi.LIGHT_7);
				break;
			case Gbi.G_MV_L7:
				gSPLight(w1, Gbi.LIGHT_8);
				break;
			case Gbi.G_MV_LOOKATX:
				break;
			case Gbi.G_MV_LOOKATY:
				break;
			}
		}
	};

	protected OpCode F3D_Vtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPVertex(segmentToPhysical(w1), ((w0 >>> 20) & Gbi.SR_MASK_4) + 1, (w0 >>> 16) & Gbi.SR_MASK_4);
		}
	};

	protected OpCode F3D_Reserved1 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3D_DList = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			switch ((w0 >> 16) & Gbi.SR_MASK_8)
			{
			case Gbi.G_DL_PUSH:
				sp_writeRegister(SP_DL_REG, segmentToPhysical(w1));
				break;
			case Gbi.G_DL_NOPUSH:
				gSPBranchList(segmentToPhysical(w1));
				break;
			}
		}
	};

	protected OpCode F3D_Reserved2 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3D_Reserved3 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3D_Sprite2D_Base = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			getWord();
			getWord();
		}
	};

	protected OpCode F3D_Tri1 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSP1Triangle(((w1 >> 16) & Gbi.SR_MASK_8) / 10,
					((w1 >> 8) & Gbi.SR_MASK_8) / 10,
					(w1 & Gbi.SR_MASK_8) / 10,
					(w1 >> 24) & Gbi.SR_MASK_8);
		}
	};

	protected OpCode F3D_CullDL = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPCullDisplayList((w0 & Gbi.SR_MASK_24) / 40, (w1 / 40) - 1);
		}
	};

	protected OpCode F3D_PopMtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPPopMatrix(w1);
		}
	};

	protected OpCode F3D_MoveWord = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			switch (w0 & Gbi.SR_MASK_8)
			{
			case Gbi.G_MW_MATRIX: // 0x00
				gSPInsertMatrix((w0 >> 8) & Gbi.SR_MASK_16, w1);
				break;
			case Gbi.G_MW_NUMLIGHT: // 0x02
				gSPNumLights(((w1 - 0x80000000) >>> 5) - 1);
				break;
			case Gbi.G_MW_CLIP: // 0x04
				break;
			case Gbi.G_MW_SEGMENT: // 0x06
				gSPSegment(((w0 >> 8) & Gbi.SR_MASK_16) >>> 2, w1 & 0x00FFFFFF);
				break;
			case Gbi.G_MW_FOG: // 0x08
				gSPFogFactor((short) ((w1 >> 16) & Gbi.SR_MASK_16), (short) (w1 & Gbi.SR_MASK_16));
				break;
			case Gbi.G_MW_LIGHTCOL: // 0x0A
				switch ((w0 >> 8) & Gbi.SR_MASK_16)
				{
				case F3D_MWO_aLIGHT_1:
					gSPLightColor(Gbi.LIGHT_1, w1);
					break;
				case F3D_MWO_aLIGHT_2:
					gSPLightColor(Gbi.LIGHT_2, w1);
					break;
				case F3D_MWO_aLIGHT_3:
					gSPLightColor(Gbi.LIGHT_3, w1);
					break;
				case F3D_MWO_aLIGHT_4:
					gSPLightColor(Gbi.LIGHT_4, w1);
					break;
				case F3D_MWO_aLIGHT_5:
					gSPLightColor(Gbi.LIGHT_5, w1);
					break;
				case F3D_MWO_aLIGHT_6:
					gSPLightColor(Gbi.LIGHT_6, w1);
					break;
				case F3D_MWO_aLIGHT_7:
					gSPLightColor(Gbi.LIGHT_7, w1);
					break;
				case F3D_MWO_aLIGHT_8:
					gSPLightColor(Gbi.LIGHT_8, w1);
					break;
				}
				break;
			case Gbi.G_MW_POINTS: // 0x0C
				gSPModifyVertex(((w0 >> 8) & Gbi.SR_MASK_16) / 40, (w0 & Gbi.SR_MASK_8) % 40, w1);
				break;
			case Gbi.G_MW_PERSPNORM: // 0x0E
				break;
			}
		}
	};

	protected OpCode F3D_Texture = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			cmdBuf[0] = 0x12;
			cmdBuf[1] = (byte) ((w0 >> 11) & Gbi.SR_MASK_3);
			cmdBuf[2] = (byte) ((w0 >> 8) & Gbi.SR_MASK_3);
			cmdBuf[3] = (byte) (w0);
			cmdBuf[4] = (byte) (w1 >> 24);
			cmdBuf[5] = (byte) (w1 >> 16);
			cmdBuf[6] = (byte) (w1 >> 8);
			cmdBuf[7] = (byte) (w1 >> 0);
			((BusDMA) dp).writeDMA(0x12, cmdBuffer, 0, 8);
		}
	};

	protected OpCode F3D_SetOtherMode_H = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			switch ((w0 >> 8) & Gbi.SR_MASK_8)
			{
			case Gbi.G_MDSFT_PIPELINE:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~PIPELINE_MODE);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_PIPELINE) & 0x1) << 23));
				break;
			case Gbi.G_MDSFT_CYCLETYPE:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~CYCLE_TYPE);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_CYCLETYPE) & 0x3) << 20));
				break;
			case Gbi.G_MDSFT_TEXTPERSP:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~TEX_PERSP);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_TEXTPERSP) & 0x1) << 19));
				break;
			case Gbi.G_MDSFT_TEXTDETAIL:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~TEX_DETAIL);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_TEXTDETAIL) & 0x3) << 17));
				break;
			case Gbi.G_MDSFT_TEXTLOD:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~TEX_LOD);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_TEXTLOD) & 0x1) << 16));
				break;
			case Gbi.G_MDSFT_TEXTLUT:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~TEX_LUT);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_TEXTLUT) & 0x3) << 14));
				break;
			case Gbi.G_MDSFT_TEXTFILT:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~TEX_FILTER);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_TEXTFILT) & 0x3) << 12));
				break;
			case Gbi.G_MDSFT_TEXTCONV:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~TEX_CONVERT);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_TEXTCONV) & 0x7) << 9));
				break;
			case Gbi.G_MDSFT_COMBKEY:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~COMBINE_KEY);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_COMBKEY) & 0x1) << 8));
				break;
			case Gbi.G_MDSFT_RGBDITHER:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~COLOR_DITHER);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_RGBDITHER) & 0x3) << 6));
				break;
			case Gbi.G_MDSFT_ALPHADITHER:
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~ALPHA_DITHER);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (((w1 >>> Gbi.G_MDSFT_ALPHADITHER) & 0x3) << 4));
				break;
			default:
				int length = w0 & Gbi.SR_MASK_8;
				int shift = (w0 >> 8) & Gbi.SR_MASK_8;
				int mask = ((1 << length) - 1) << shift;

				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~mask);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (w1 & mask));
				break;
			}
		}
	};

	protected OpCode F3D_SetOtherMode_L = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			switch ((w0 >> 8) & Gbi.SR_MASK_8)
			{
			case Gbi.G_MDSFT_ALPHACOMPARE:
				dp_writeRegister(DP_OTHERMODE_LO_REG, dp_readRegister(DP_OTHERMODE_LO_REG) & ~ALPHA_COMPARE);
				dp_writeRegister(DP_OTHERMODE_LO_REG, dp_readRegister(DP_OTHERMODE_LO_REG) | (((w1 >>> Gbi.G_MDSFT_ALPHACOMPARE) & 0x3) << 0));
				break;
			case Gbi.G_MDSFT_ZSRCSEL:
				dp_writeRegister(DP_OTHERMODE_LO_REG, dp_readRegister(DP_OTHERMODE_LO_REG) & ~DEPTH_SOURCE);
				dp_writeRegister(DP_OTHERMODE_LO_REG, dp_readRegister(DP_OTHERMODE_LO_REG) | (((w1 >>> Gbi.G_MDSFT_ZSRCSEL) & 0x1) << 2));
				break;
			case Gbi.G_MDSFT_RENDERMODE:
				dp_writeRegister(DP_OTHERMODE_LO_REG, dp_readRegister(DP_OTHERMODE_LO_REG) & 0x00000007);
				dp_writeRegister(DP_OTHERMODE_LO_REG, dp_readRegister(DP_OTHERMODE_LO_REG) | (w1 & 0xCCCCFFFF) | (w1 & 0x3333FFFF));
				break;
			default:
				int shift = (w0 >> 8) & Gbi.SR_MASK_8;
				int length = w0 & Gbi.SR_MASK_8;
				int mask = ((1 << length) - 1) << shift;

				dp_writeRegister(DP_OTHERMODE_LO_REG, dp_readRegister(DP_OTHERMODE_LO_REG) & ~mask);
				dp_writeRegister(DP_OTHERMODE_LO_REG, dp_readRegister(DP_OTHERMODE_LO_REG) | (w1 & mask));
				break;
			}
		}
	};

	protected OpCode F3D_EndDL = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			sp_readRegister(SP_DL_REG);
		}
	};

	protected OpCode F3D_SetGeometryMode = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPSetGeometryMode(w1);
		}
	};

	protected OpCode F3D_ClearGeometryMode = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPClearGeometryMode(w1);
		}
	};

	protected OpCode F3D_Line3D = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			// Hmmm...
		}
	};

	protected OpCode F3D_Quad = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSP1Quadrangle(((w1 >> 24) & Gbi.SR_MASK_8) / 10, ((w1 >> 16) & Gbi.SR_MASK_8) / 10, ((w1 >> 8) & Gbi.SR_MASK_8) / 10, (w1 & Gbi.SR_MASK_8) / 10);
		}
	};

	protected OpCode F3D_RDPHalf_1 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			half_1 = w1;
		}
	};

	protected OpCode F3D_RDPHalf_2 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			half_2 = w1;
		}
	};

	protected OpCode F3D_RDPHalf_Cont = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3D_Tri4 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSP4Triangles(w0 & Gbi.SR_MASK_4, w1 & Gbi.SR_MASK_4, (w1 >> 4) & Gbi.SR_MASK_4,
					(w0 >> 4) & Gbi.SR_MASK_4, (w1 >> 8) & Gbi.SR_MASK_4, (w1 >> 12) & Gbi.SR_MASK_4,
					(w0 >> 8) & Gbi.SR_MASK_4, (w1 >> 16) & Gbi.SR_MASK_4, (w1 >> 20) & Gbi.SR_MASK_4,
					(w0 >> 12) & Gbi.SR_MASK_4, (w1 >> 24) & Gbi.SR_MASK_4, (w1 >> 28) & Gbi.SR_MASK_4);
		}
	};
}
