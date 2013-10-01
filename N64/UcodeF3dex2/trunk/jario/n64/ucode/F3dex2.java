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
import jario.n64.ucode.F3dex;
import jario.n64.ucode.Gbi;

public class F3dex2 extends F3dex
{
	public static final int F3DEX2_MTX_STACKSIZE = 18;

	public static final int F3DEX2_MTX_MODELVIEW = 0x00;
	public static final int F3DEX2_MTX_PROJECTION = 0x04;
	public static final int F3DEX2_MTX_MUL = 0x00;
	public static final int F3DEX2_MTX_LOAD = 0x02;
	public static final int F3DEX2_MTX_NOPUSH = 0x00;
	public static final int F3DEX2_MTX_PUSH = 0x01;

	public static final int F3DEX2_TEXTURE_ENABLE = 0x00000000;
	public static final int F3DEX2_SHADING_SMOOTH = 0x00200000;
	public static final int F3DEX2_CULL_FRONT = 0x00000200;
	public static final int F3DEX2_CULL_BACK = 0x00000400;
	public static final int F3DEX2_CULL_BOTH = 0x00000600;
	public static final int F3DEX2_CLIPPING = 0x00800000;

	public static final int F3DEX2_MV_VIEWPORT = 8;

	public static final int F3DEX2_MWO_aLIGHT_1 = 0x00;
	public static final int F3DEX2_MWO_bLIGHT_1 = 0x04;
	public static final int F3DEX2_MWO_aLIGHT_2 = 0x18;
	public static final int F3DEX2_MWO_bLIGHT_2 = 0x1c;
	public static final int F3DEX2_MWO_aLIGHT_3 = 0x30;
	public static final int F3DEX2_MWO_bLIGHT_3 = 0x34;
	public static final int F3DEX2_MWO_aLIGHT_4 = 0x48;
	public static final int F3DEX2_MWO_bLIGHT_4 = 0x4c;
	public static final int F3DEX2_MWO_aLIGHT_5 = 0x60;
	public static final int F3DEX2_MWO_bLIGHT_5 = 0x64;
	public static final int F3DEX2_MWO_aLIGHT_6 = 0x78;
	public static final int F3DEX2_MWO_bLIGHT_6 = 0x7c;
	public static final int F3DEX2_MWO_aLIGHT_7 = 0x90;
	public static final int F3DEX2_MWO_bLIGHT_7 = 0x94;
	public static final int F3DEX2_MWO_aLIGHT_8 = 0xa8;
	public static final int F3DEX2_MWO_bLIGHT_8 = 0xac;

	public static final int F3DEX2_RDPHALF_2 = 0xF1;
	public static final int F3DEX2_SETOTHERMODE_H = 0xE3;
	public static final int F3DEX2_SETOTHERMODE_L = 0xE2;
	public static final int F3DEX2_RDPHALF_1 = 0xE1;
	public static final int F3DEX2_SPNOOP = 0xE0;
	public static final int F3DEX2_ENDDL = 0xDF;
	public static final int F3DEX2_DL = 0xDE;
	public static final int F3DEX2_LOAD_UCODE = 0xDD;
	public static final int F3DEX2_MOVEMEM = 0xDC;
	public static final int F3DEX2_MOVEWORD = 0xDB;
	public static final int F3DEX2_MTX = 0xDA;
	public static final int F3DEX2_GEOMETRYMODE = 0xD9;
	public static final int F3DEX2_POPMTX = 0xD8;
	public static final int F3DEX2_TEXTURE = 0xD7;
	public static final int F3DEX2_DMA_IO = 0xD6;
	public static final int F3DEX2_SPECIAL_1 = 0xD5;
	public static final int F3DEX2_SPECIAL_2 = 0xD4;
	public static final int F3DEX2_SPECIAL_3 = 0xD3;

	public static final int F3DEX2_VTX = 0x01;
	public static final int F3DEX2_MODIFYVTX = 0x02;
	public static final int F3DEX2_CULLDL = 0x03;
	public static final int F3DEX2_BRANCH_Z = 0x04;
	public static final int F3DEX2_TRI1 = 0x05;
	public static final int F3DEX2_TRI2 = 0x06;
	public static final int F3DEX2_QUAD = 0x07;

	public F3dex2()
	{
	}

	@Override
	public void clock(long ticks)
	{
		gbiInitFlags();

		Gbi.G_TRI1 = F3DEX2_TRI1;
		Gbi.G_TRI2 = F3DEX2_TRI2;
		Gbi.G_QUAD = F3DEX2_QUAD;

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
		dlist[0x05] = F3DEX2_Tri1;
		dlist[0x06] = F3DEX_Tri2;
		dlist[0x07] = F3DEX2_Quad;
	}

	// Protected Methods ///////////////////////////////////////////////////////

	protected static void gbiInitFlags()
	{
		Gbi.G_MTX_PROJECTION = F3DEX2_MTX_PROJECTION;
		Gbi.G_MTX_LOAD = F3DEX2_MTX_LOAD;
		Gbi.G_MTX_PUSH = F3DEX2_MTX_PUSH;
		Gbi.G_CULL_BACK = F3DEX2_CULL_BACK;
		Gbi.G_CULL_BOTH = F3DEX2_CULL_BOTH;
	}

	protected void gSPPopMatrixN(int param, int num)
	{
		if (matrix.modelViewi > num - 1)
		{
			matrix.modelViewi -= num;
			changed |= CHANGED_MATRIX;
		}
	}

	/************************* OpCode functions *************************/

	protected OpCode F3DEX2_Mtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPMatrix(segmentToPhysical(w1), (w0 & Gbi.SR_MASK_8) ^ Gbi.G_MTX_PUSH);
		}
	};

	protected OpCode F3DEX2_MoveMem = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			switch (w0 & Gbi.SR_MASK_8)
			{
			case F3DEX2_MV_VIEWPORT:
				int addr = segmentToPhysical(w1);
				if ((addr + 16) <= rdramSize)
					dp_writeRegister(DP_VIEWPORT_REG, addr);
				break;
			case Gbi.G_MV_MATRIX:
				gSPForceMatrix(segmentToPhysical(w1));
				// force matrix takes two commands
				getWord();
				getWord();
				break;
			case Gbi.G_MV_LIGHT:
				int offset = ((w0 >> 8) & Gbi.SR_MASK_8) << 3;
				if (offset >= 48)
				{
					gSPLight(segmentToPhysical(w1), (offset - 24) / 24);
				}
				break;
			}
		}
	};

	protected OpCode F3DEX2_Vtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			int n = (w0 >>> 12) & Gbi.SR_MASK_8;
			gSPVertex(segmentToPhysical(w1), n, ((w0 >>> 1) & Gbi.SR_MASK_7) - n);
		}
	};

	protected OpCode F3DEX2_Reserved1 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3DEX2_Tri1 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSP1Triangle((w0 >> 17) & Gbi.SR_MASK_7,
					(w0 >> 9) & Gbi.SR_MASK_7,
					(w0 >> 1) & Gbi.SR_MASK_7,
					0);
		}
	};

	protected OpCode F3DEX2_PopMtx = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPPopMatrixN(0, w1 >>> 6);
		}
	};

	protected OpCode F3DEX2_MoveWord = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			switch ((w0 >> 16) & Gbi.SR_MASK_8)
			{
			case Gbi.G_MW_FORCEMTX:
				// Handled in movemem
				break;
			case Gbi.G_MW_MATRIX:
				gSPInsertMatrix(w0 & Gbi.SR_MASK_16, w1);
				break;
			case Gbi.G_MW_NUMLIGHT:
				gSPNumLights(w1 / 24);
				break;
			case Gbi.G_MW_CLIP:
				// gSPClipRatio(w1);
				break;
			case Gbi.G_MW_SEGMENT:
				gSPSegment((w0 & Gbi.SR_MASK_16) >>> 2, w1 & 0x00FFFFFF);
				break;
			case Gbi.G_MW_FOG:
				gSPFogFactor((short) ((w1 >> 16) & Gbi.SR_MASK_16), (short) (w1 & Gbi.SR_MASK_16));
				break;
			case Gbi.G_MW_LIGHTCOL:
				switch (w0 & Gbi.SR_MASK_16)
				{
				case F3DEX2_MWO_aLIGHT_1:
					gSPLightColor(Gbi.LIGHT_1, w1);
					break;
				case F3DEX2_MWO_aLIGHT_2:
					gSPLightColor(Gbi.LIGHT_2, w1);
					break;
				case F3DEX2_MWO_aLIGHT_3:
					gSPLightColor(Gbi.LIGHT_3, w1);
					break;
				case F3DEX2_MWO_aLIGHT_4:
					gSPLightColor(Gbi.LIGHT_4, w1);
					break;
				case F3DEX2_MWO_aLIGHT_5:
					gSPLightColor(Gbi.LIGHT_5, w1);
					break;
				case F3DEX2_MWO_aLIGHT_6:
					gSPLightColor(Gbi.LIGHT_6, w1);
					break;
				case F3DEX2_MWO_aLIGHT_7:
					gSPLightColor(Gbi.LIGHT_7, w1);
					break;
				case F3DEX2_MWO_aLIGHT_8:
					gSPLightColor(Gbi.LIGHT_8, w1);
					break;
				}
				break;
			case Gbi.G_MW_PERSPNORM:
				// gSPPerspNormalize((short)w1);
				break;
			}
		}
	};

	protected OpCode F3DEX2_Texture = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			cmdBuf[0] = 0x12;
			cmdBuf[1] = (byte) ((w0 >> 11) & Gbi.SR_MASK_3);
			cmdBuf[2] = (byte) ((w0 >> 8) & Gbi.SR_MASK_3);
			cmdBuf[3] = (byte) ((w0 >> 1) & Gbi.SR_MASK_7);
			cmdBuf[4] = (byte) (w1 >> 24);
			cmdBuf[5] = (byte) (w1 >> 16);
			cmdBuf[6] = (byte) (w1 >> 8);
			cmdBuf[7] = (byte) (w1 >> 0);
			((BusDMA) dp).writeDMA(0x12, cmdBuffer, 0, 8);
		}
	};

	protected OpCode F3DEX2_SetOtherMode_H = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			switch (32 - ((w0 >> 8) & Gbi.SR_MASK_8) - ((w0 & Gbi.SR_MASK_8) + 1))
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
				int length = (w0 & Gbi.SR_MASK_8) + 1;
				int shift = 32 - ((w0 >> 8) & Gbi.SR_MASK_8) - length;
				int mask = ((1 << length) - 1) << shift;

				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) & ~mask);
				dp_writeRegister(DP_OTHERMODE_HI_REG, dp_readRegister(DP_OTHERMODE_HI_REG) | (w1 & mask));
				break;
			}
		}
	};

	protected OpCode F3DEX2_SetOtherMode_L = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			switch (32 - ((w0 >> 8) & Gbi.SR_MASK_8) - ((w0 & Gbi.SR_MASK_8) + 1))
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
				int length = (w0 & Gbi.SR_MASK_8) + 1;
				int shift = 32 - ((w0 >> 8) & Gbi.SR_MASK_8) - length;
				int mask = ((1 << length) - 1) << shift;

				dp_writeRegister(DP_OTHERMODE_LO_REG, dp_readRegister(DP_OTHERMODE_LO_REG) & ~mask);
				dp_writeRegister(DP_OTHERMODE_LO_REG, dp_readRegister(DP_OTHERMODE_LO_REG) | (w1 & mask));
				break;
			}
		}
	};

	protected OpCode F3DEX2_GeometryMode = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSPGeometryMode(~(w0 & Gbi.SR_MASK_24), w1);
		}
	};

	protected OpCode F3DEX2_DMAIO = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3DEX2_Special_1 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3DEX2_Special_2 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3DEX2_Special_3 = new OpCode()
	{
		public void exec(int w0, int w1)
		{
		}
	};

	protected OpCode F3DEX2_Quad = new OpCode()
	{
		public void exec(int w0, int w1)
		{
			gSP2Triangles((w0 >> 17) & Gbi.SR_MASK_7,
					(w0 >> 9) & Gbi.SR_MASK_7,
					(w0 >> 1) & Gbi.SR_MASK_7,
					0,
					(w1 >> 17) & Gbi.SR_MASK_7,
					(w1 >> 9) & Gbi.SR_MASK_7,
					(w1 >> 1) & Gbi.SR_MASK_7,
					0);
		}
	};
}
