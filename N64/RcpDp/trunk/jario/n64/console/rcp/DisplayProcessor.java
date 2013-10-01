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

package jario.n64.console.rcp;

import jario.hardware.Bus16bit;
import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.n64.console.rcp.combiners.Combiners;
import jario.n64.console.rcp.textures.TextureCache;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Random;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import com.jogamp.common.nio.Buffers;
import javax.media.opengl.awt.GLCanvas;

public class DisplayProcessor implements Hardware, Clockable, Bus32bit, BusDMA
{
	private static final boolean debug = false;
	private static final boolean wireframe = false;

	private static final int SIZEOF_FLOAT = 4;
	private static final int SIZEOF_GLVERTEX = 17 * SIZEOF_FLOAT;

	// private static final int VI_STATUS_REG = 0;
	// private static final int VI_ORIGIN_REG = 1;
	// private static final int VI_WIDTH_REG = 2;
	// private static final int VI_INTR_REG = 3;
	// private static final int VI_CURRENT_REG = 4;
	// private static final int VI_BURST_REG = 5;
	// private static final int VI_V_SYNC_REG = 6;
	// private static final int VI_H_SYNC_REG = 7;
	// private static final int VI_LEAP_REG = 8;
	private static final int VI_H_START_REG = 9;
	private static final int VI_V_START_REG = 10;
	// private static final int VI_V_BURST_REG = 11;
	private static final int VI_X_SCALE_REG = 12;
	private static final int VI_Y_SCALE_REG = 13;
	// private static final int VI_DISPLAYABLE_REG = 15;

	private static final int MI_INTR_REG = 0x04300008;

	// private static final int MI_INTR_CLR_DP = 0x0400; /* Bit 10: clear DP interrupt */
	private static final int MI_INTR_SET_DP = 0x0800; /* Bit 11: set DP interrupt */
	// private static final int MI_INTR_MASK_CLR_DP = 0x0400; /* Bit 10: clear DP mask */
	// private static final int MI_INTR_MASK_SET_DP = 0x0800; /* Bit 11: set DP mask */
	// private static final int MI_INTR_MASK_DP = 0x20; /* Bit 5: DP intr mask */
	// private static final int MI_INTR_DP = 0x20; /* Bit 5: DP intr */

	// DP Command Registers
	private static final int DPC_START_REG = 0;
	private static final int DPC_END_REG = 1;
	private static final int DPC_CURRENT_REG = 2;
	private static final int DPC_STATUS_REG = 3;
	private static final int DPC_CLOCK_REG = 4;
	private static final int DPC_BUFBUSY_REG = 5;
	private static final int DPC_PIPEBUSY_REG = 6;
	private static final int DPC_TMEM_REG = 7;

	private static final int DPC_CLR_XBUS_DMEM_DMA = 0x0001; /* Bit 0: clear xbus_dmem_dma */
	private static final int DPC_SET_XBUS_DMEM_DMA = 0x0002; /* Bit 1: set xbus_dmem_dma */
	private static final int DPC_CLR_FREEZE = 0x0004; /* Bit 2: clear freeze */
	private static final int DPC_SET_FREEZE = 0x0008; /* Bit 3: set freeze */
	private static final int DPC_CLR_FLUSH = 0x0010; /* Bit 4: clear flush */
	private static final int DPC_SET_FLUSH = 0x0020; /* Bit 5: set flush */
	// private static final int DPC_CLR_TMEM_CTR = 0x0040; /* Bit 6: clear tmem ctr */
	// private static final int DPC_CLR_PIPE_CTR = 0x0080; /* Bit 7: clear pipe ctr */
	// private static final int DPC_CLR_CMD_CTR = 0x0100; /* Bit 8: clear cmd ctr */
	// private static final int DPC_CLR_CLOCK_CTR = 0x0200; /* Bit 9: clear clock ctr */

	private static final int DPC_STATUS_XBUS_DMEM_DMA = 0x001; /* Bit 0: xbus_dmem_dma */
	private static final int DPC_STATUS_FREEZE = 0x002; /* Bit 1: freeze */
	private static final int DPC_STATUS_FLUSH = 0x004; /* Bit 2: flush */
	// private static final int DPC_STATUS_START_GCLK = 0x008; /* Bit 3: start gclk */
	// private static final int DPC_STATUS_TMEM_BUSY = 0x010; /* Bit 4: tmem busy */
	// private static final int DPC_STATUS_PIPE_BUSY = 0x020; /* Bit 5: pipe busy */
	// private static final int DPC_STATUS_CMD_BUSY = 0x040; /* Bit 6: cmd busy */
	// private static final int DPC_STATUS_CBUF_READY = 0x080; /* Bit 7: cbuf ready */
	// private static final int DPC_STATUS_DMA_BUSY = 0x100; /* Bit 8: dma busy */
	// private static final int DPC_STATUS_END_VALID = 0x200; /* Bit 9: end valid */
	// private static final int DPC_STATUS_START_VALID = 0x400; /* Bit 10: start valid */

	private static final int CHANGED_RENDERMODE = 0x001;
	private static final int CHANGED_CYCLETYPE = 0x002;
	private static final int CHANGED_SCISSOR = 0x004;
	// private static final int CHANGED_COLORBUFFER = 0x008;
	private static final int CHANGED_TILE = 0x010;
	private static final int CHANGED_COMBINE_COLORS = 0x020;
	private static final int CHANGED_COMBINE = 0x040;
	private static final int CHANGED_ALPHACOMPARE = 0x080;
	private static final int CHANGED_FOGCOLOR = 0x100;
	private static final int CHANGED_TEXTURE = 0x200;
	private static final int CHANGED_VIEWPORT = 0x400;

	// Fixed point conversion factors
	// private static final float FIXED2FLOATRECIP1 = 0.5f;
	private static final float FIXED2FLOATRECIP2 = 0.25f;
	// private static final float FIXED2FLOATRECIP3 = 0.125f;
	// private static final float FIXED2FLOATRECIP4 = 0.0625f;
	private static final float FIXED2FLOATRECIP5 = 0.03125f;
	// private static final float FIXED2FLOATRECIP6 = 0.015625f;
	// private static final float FIXED2FLOATRECIP7 = 0.0078125f;
	// private static final float FIXED2FLOATRECIP8 = 0.00390625f;
	// private static final float FIXED2FLOATRECIP9 = 0.001953125f;
	private static final float FIXED2FLOATRECIP10 = 0.0009765625f;
	// private static final float FIXED2FLOATRECIP11 = 0.00048828125f;
	// private static final float FIXED2FLOATRECIP12 = 0.00024414063f;
	// private static final float FIXED2FLOATRECIP13 = 0.00012207031f;
	// private static final float FIXED2FLOATRECIP14 = 6.1035156e-05f;
	// private static final float FIXED2FLOATRECIP15 = 3.0517578e-05f;
	// private static final float FIXED2FLOATRECIP16 = 1.5258789e-05f;

	/*
	 * G_SETOTHERMODE_L sft: shift count
	 */
	// private static final int G_MDSFT_ALPHACOMPARE = 0;
	// private static final int G_MDSFT_ZSRCSEL = 2;
	// private static final int G_MDSFT_RENDERMODE = 3;
	// private static final int G_MDSFT_BLENDER = 16;

	/*
	 * G_SETOTHERMODE_H sft: shift count
	 */
	// private static final int G_MDSFT_BLENDMASK = 0; /* unsupported */
	// private static final int G_MDSFT_ALPHADITHER = 4;
	// private static final int G_MDSFT_RGBDITHER = 6;

	// private static final int G_MDSFT_COMBKEY = 8;
	// private static final int G_MDSFT_TEXTCONV = 9;
	// private static final int G_MDSFT_TEXTFILT = 12;
	// private static final int G_MDSFT_TEXTLUT = 14;
	// private static final int G_MDSFT_TEXTLOD = 16;
	// private static final int G_MDSFT_TEXTDETAIL = 17;
	// private static final int G_MDSFT_TEXTPERSP = 19;
	// private static final int G_MDSFT_CYCLETYPE = 20;
	// private static final int G_MDSFT_COLORDITHER = 22; /* unsupported in HW 2.0 */
	// private static final int G_MDSFT_PIPELINE = 23;

	/* G_SETOTHERMODE_H gPipelineMode */
	// private static final int G_PM_1PRIMITIVE = 1;
	private static final int G_PM_NPRIMITIVE = 0;

	/* G_SETOTHERMODE_H gSetCycleType */
	private static final int G_CYC_1CYCLE = 0;
	private static final int G_CYC_2CYCLE = 1;
	private static final int G_CYC_COPY = 2;
	private static final int G_CYC_FILL = 3;

	/* G_SETOTHERMODE_H gSetTexturePersp */
	// private static final int G_TP_NONE = 0;
	private static final int G_TP_PERSP = 1;

	/* G_SETOTHERMODE_H gSetTextureDetail */
	private static final int G_TD_CLAMP = 0;
	// private static final int G_TD_SHARPEN = 1;
	// private static final int G_TD_DETAIL = 2;

	/* G_SETOTHERMODE_H gSetTextureLOD */
	private static final int G_TL_TILE = 0;
	// private static final int G_TL_LOD = 1;

	/* G_SETOTHERMODE_H gSetTextureLUT */
	private static final int G_TT_NONE = 0;
	// private static final int G_TT_RGBA16 = 2;
	// private static final int G_TT_IA16 = 3;

	/* G_SETOTHERMODE_H gSetTextureFilter */
	private static final int G_TF_POINT = 0;
	// private static final int G_TF_AVERAGE = 3;
	// private static final int G_TF_BILERP = 2;

	/* G_SETOTHERMODE_H gSetTextureConvert */
	// private static final int G_TC_CONV = 0;
	// private static final int G_TC_FILTCONV = 5;
	private static final int G_TC_FILT = 6;

	/* G_SETOTHERMODE_H gSetCombineKey */
	private static final int G_CK_NONE = 0;
	// private static final int G_CK_KEY = 1;

	/* G_SETOTHERMODE_H gSetColorDither */
	// private static final int G_CD_MAGICSQ = 0;
	// private static final int G_CD_BAYER = 1;
	// private static final int G_CD_NOISE = 2;

	private static final int G_CD_DISABLE = 3;
	// private static final int G_CD_ENABLE = G_CD_NOISE; /* HW 1.0 compatibility mode */

	/* G_SETOTHERMODE_H gSetAlphaDither */
	// private static final int G_AD_PATTERN = 0;
	// private static final int G_AD_NOTPATTERN = 1;
	// private static final int G_AD_NOISE = 2;
	private static final int G_AD_DISABLE = 3;

	/* G_SETOTHERMODE_L gSetAlphaCompare */
	private static final int G_AC_NONE = 0;
	private static final int G_AC_THRESHOLD = 1;
	private static final int G_AC_DITHER = 3;

	/* G_SETOTHERMODE_L gSetDepthSource */
	private static final int G_ZS_PIXEL = 0;
	private static final int G_ZS_PRIM = 1;

	/* G_SETOTHERMODE_L gSetRenderMode */
	// private static final int AA_EN = 1;
	// private static final int Z_CMP = 1;
	// private static final int Z_UPD = 1;
	// private static final int IM_RD = 1;
	// private static final int CLR_ON_CVG = 1;
	// private static final int CVG_DST_CLAMP = 0;
	// private static final int CVG_DST_WRAP = 1;
	// private static final int CVG_DST_FULL = 2;
	// private static final int CVG_DST_SAVE = 3;
	// private static final int ZMODE_OPA = 0;
	// private static final int ZMODE_INTER = 1;
	// private static final int ZMODE_XLU = 2;
	private static final int ZMODE_DEC = 3;
	// private static final int CVG_X_ALPHA = 1;
	// private static final int ALPHA_CVG_SEL = 1;
	// private static final int FORCE_BL = 1;
	// private static final int TEX_EDGE = 0; // not used

	private static final int PIPELINE_MODE = 0x800000;
	private static final int CYCLE_TYPE = 0x300000;
	private static final int TEX_PERSP = 0x80000;
	private static final int TEX_DETAIL = 0x60000;
	private static final int TEX_LOD = 0x10000;
	private static final int TEX_LUT = 0xC000;
	private static final int TEX_FILTER = 0x3000;
	private static final int TEX_CONVERT = 0xE00;
	private static final int COMBINE_KEY = 0x100;
	private static final int COLOR_DITHER = 0xC0;
	private static final int ALPHA_DITHER = 0x30;
	private static final int DEPTH_SOURCE = 0x4;
	private static final int ALPHA_COMPARE = 0x3;

	private static final int RDP_GETOM_CYCLE_TYPE(OtherMode om)
	{
		return (((om).w0 >> 20) & 0x3);
	}

	private static final int RDP_GETOM_FORCE_BLEND(OtherMode om)
	{
		return (((om).w1 & 0x4000) != 0 ? 1 : 0);
	}

	private static final int RDP_GETOM_ALPHA_CVG_SELECT(OtherMode om)
	{
		return (((om).w1 & 0x2000) != 0 ? 1 : 0);
	}

	private static final int RDP_GETOM_CVG_TIMES_ALPHA(OtherMode om)
	{
		return (((om).w1 & 0x1000) != 0 ? 1 : 0);
	}

	private static final int RDP_GETOM_Z_MODE(OtherMode om)
	{
		return (((om).w1 >> 10) & 0x3);
	}

	private static final int RDP_GETOM_Z_UPDATE_EN(OtherMode om)
	{
		return (((om).w1 & 0x20) != 0 ? 1 : 0);
	}

	private static final int RDP_GETOM_Z_COMPARE_EN(OtherMode om)
	{
		return (((om).w1 & 0x10) != 0 ? 1 : 0);
	}

	private static final int RDP_GETOM_Z_SOURCE_SEL(OtherMode om)
	{
		return (((om).w1 & 0x04) != 0 ? 1 : 0);
	}

	private static final int RDP_GETOM_ALPHA_COMPARE_EN(OtherMode om)
	{
		return (((om).w1 >> 0) & 0x3);
	}

	private static class OtherMode
	{
		public int w0; // high
		public int w1; // low
	};

	private static class FillColor
	{
		public float[] color = new float[4];
		// public float z;
		// public float dz;
	};

	private static class PrimDepth
	{
		public float z;
		// public float deltaZ;
	};

	private static class ColorImage
	{
		// public int format;
		// public int size;
		// public int width;
		public int height;
		// public int bpl;
		public int address;
		// public boolean changed;
		// public int depthImage;
	};

	private static class Scissor
	{
		// public int mode;
		public float ulx;
		public float uly;
		public float lrx;
		public float lry;
	};

	// private static class Convert {
	// public int k0;
	// public int k1;
	// public int k2;
	// public int k3;
	// public int k4;
	// public int k5;
	// };

	// private static class Key {
	// public float[] center = new float[4];
	// public float[] scale = new float[4];
	// public float[] width = new float[4];
	// };

	private static class Vertex
	{
		public float[] vtx = new float[4];
		public float[] color = new float[4];
		public float[] tex = new float[2];
		public float[] clip = new float[3];

		public void copyVertex(Vertex src)
		{
			vtx[0] = src.vtx[0];
			vtx[1] = src.vtx[1];
			vtx[2] = src.vtx[2];
			vtx[3] = src.vtx[3];
			color[0] = src.color[0];
			color[1] = src.color[1];
			color[2] = src.color[2];
			color[3] = src.color[3];
			tex[0] = src.tex[0];
			tex[1] = src.tex[1];
		}

		public void interpolateVertex(float percent, Vertex first, Vertex second)
		{
			vtx[0] = first.vtx[0] + percent * (second.vtx[0] - first.vtx[0]);
			vtx[1] = first.vtx[1] + percent * (second.vtx[1] - first.vtx[1]);
			vtx[2] = first.vtx[2] + percent * (second.vtx[2] - first.vtx[2]);
			vtx[3] = first.vtx[3] + percent * (second.vtx[3] - first.vtx[3]);
			color[0] = first.color[0] + percent * (second.color[0] - first.color[0]);
			color[1] = first.color[1] + percent * (second.color[1] - first.color[1]);
			color[2] = first.color[2] + percent * (second.color[2] - first.color[2]);
			color[3] = first.color[3] + percent * (second.color[3] - first.color[3]);
			tex[0] = first.tex[0] + percent * (second.tex[0] - first.tex[0]);
			tex[1] = first.tex[1] + percent * (second.tex[1] - first.tex[1]);
		}
	};

	private static class GLSimpleVertex
	{
		public float x, y, z;
		public float w;
		public float[] color = new float[4]; // r,g,b,a
		public float[] secondaryColor = new float[4]; // r,g,b,a
		public float s0, t0, s1, t1;
		public float fog;
	};

	private static class GLVertex
	{
		public FloatBuffer vtx; // 4
		public FloatBuffer color; // 4
		public FloatBuffer secondaryColor; // 4
		public FloatBuffer tex0; // 2
		public FloatBuffer tex1; // 2
		public FloatBuffer fog; // 1

		public GLVertex()
		{
		}

		// public GLVertex(float x, float y, float z, float w,
		// float r1, float g1, float b1, float a1,
		// float r2, float g2, float b2, float a2,
		// float s0, float t0, float s1, float t1, float f) {
		// vtx = FloatBuffer.allocate(4);
		// vtx.put(x);
		// vtx.put(y);
		// vtx.put(z);
		// vtx.put(w);
		// color = FloatBuffer.allocate(4);
		// color.put(r1);
		// color.put(g1);
		// color.put(b1);
		// color.put(a1);
		// secondaryColor = FloatBuffer.allocate(4);
		// secondaryColor.put(r2);
		// secondaryColor.put(g2);
		// secondaryColor.put(b2);
		// secondaryColor.put(a2);
		// tex0 = FloatBuffer.allocate(2);
		// tex0.put(s0);
		// tex0.put(t0);
		// tex1 = FloatBuffer.allocate(2);
		// tex1.put(s1);
		// tex1.put(t1);
		// fog = FloatBuffer.allocate(1);
		// fog.put(f);
		// }
	};

	public static interface OpCode
	{
		public void exec(int inst1, int inst2);
	}

	private static GL2 gl;
	private static TextureCache cache;
	private static int width;
	private static int height;
	private static int windowedWidth;
	private static int windowedHeight;

	private GLCanvas canvas;
	private GLDrawable hDC;
	private GLContext context;
	private boolean colorbufferChanged;
	private boolean render;
	// private int DList;
	private int changed;
	private OtherMode otherMode = new OtherMode();
	private float[] fogColor = new float[4];
	private float[] blendColor = new float[4];
	private Scissor scissor = new Scissor();
	private ColorImage colorImage = new ColorImage();
	private PrimDepth primDepth = new PrimDepth();
	private DepthBufferStack depthBuffers = new DepthBufferStack();
	private DepthBufferStack.DepthBuffer current;
	private int depthImageAddress;
	private FillColor fillColor = new FillColor();
	private Combiners combiners = new Combiners();
	private ByteBuffer tmem = ByteBuffer.allocate(8 * 512);
	private OpCode[] rdp_command_table;
	private byte[] rdp_cmd_data;
	private int numTriangles;
	private GLVertex[] vertices = new GLVertex[256];
	private FloatBuffer bigArray;
	private float zDepth;
	private int numVertices;
	private float nearZ;
	private float vTrans;
	private float vScale;
	private int heightOffset;
	private boolean usePolygonStipple;
	private byte[][][] stipplePattern = new byte[32][8][128];
	private int lastStipple;
	private final GLSimpleVertex rect0 = new GLSimpleVertex();
	private final GLSimpleVertex rect1 = new GLSimpleVertex();
	private int screenWidth;
	private int screenHeight;
	private float scaleX;
	private float scaleY;

	private int[] regDPC = new int[10];

	private Bus16bit rdram;
	private Bus32bit mi;
	private Bus32bit vi;

	public DisplayProcessor()
	{
		cache = new TextureCache();
		cache.construct();
		current = null;
		depthBuffers.init();

		for (int i = 0; i < 256; i++)
			vertices[i] = new GLVertex();
		bigArray = Buffers.newDirectFloatBuffer(256 * 17);
		for (int i = 0; i < 256; i++)
		{
			bigArray.position(i * 17);
			vertices[i].vtx = bigArray.slice();
			bigArray.position(i * 17 + 4);
			vertices[i].color = bigArray.slice();
			bigArray.position(i * 17 + 8);
			vertices[i].secondaryColor = bigArray.slice();
			bigArray.position(i * 17 + 12);
			vertices[i].tex0 = bigArray.slice();
			bigArray.position(i * 17 + 14);
			vertices[i].tex1 = bigArray.slice();
			bigArray.position(i * 17 + 16);
			vertices[i].fog = bigArray.slice();
		}

		Random rand = new Random();
		for (int i = 0; i < 32; i++)
		{
			for (int j = 0; j < 8; j++)
			{
				for (int k = 0; k < 128; k++)
				{
					stipplePattern[i][j][k] = (byte) (((i > (rand.nextInt() >>> 10) ? 1 : 0) << 7) |
							((i > (rand.nextInt() >>> 10) ? 1 : 0) << 6) |
							((i > (rand.nextInt() >>> 10) ? 1 : 0) << 5) |
							((i > (rand.nextInt() >>> 10) ? 1 : 0) << 4) |
							((i > (rand.nextInt() >>> 10) ? 1 : 0) << 3) |
							((i > (rand.nextInt() >>> 10) ? 1 : 0) << 2) |
							((i > (rand.nextInt() >>> 10) ? 1 : 0) << 1) |
							((i > (rand.nextInt() >>> 10) ? 1 : 0) << 0));
				}
			}
		}
		usePolygonStipple = false;

		cache.config(32 * 1048576, 1);
		windowedWidth = 640;
		windowedHeight = 480;
		width = windowedWidth;
		height = windowedHeight;

		canvas = new GLCanvas();
		canvas.setSize(640, 480);

		buildOps();
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			rdram = (Bus16bit) bus;
			break;
		case 1:
			mi = (Bus32bit) bus;
			break;
		case 2:
			vi = (Bus32bit) bus;
			if (vi != null)
			{
				((Configurable) vi).writeConfig("screen", canvas);
				resetDp();
			}
			break;
		case 3:
			break; // timing
		default:
			System.err.println("Attempting to connect DP bus on invalid port: " + port);
			break;
		}
	}

	@Override
	public void reset()
	{
		cache.construct();
		current = null;
		depthBuffers.init();
		usePolygonStipple = false;
		cache.config(32 * 1048576, 1);
	}

	@Override
	public void clock(long ticks)
	{
		// DList++;
		colorbufferChanged = true;
		resetDp();
		context.release();
	}

	@Override
	public int read32bit(int reg)
	{
		switch ((reg - 0x04100000) >> 2)
		{
		case 3:
			return regDPC[DPC_STATUS_REG];
		case 4:
			return regDPC[DPC_CLOCK_REG];
		case 5:
			return regDPC[DPC_BUFBUSY_REG];
		case 6:
			return regDPC[DPC_PIPEBUSY_REG];
		case 7:
			return regDPC[DPC_TMEM_REG];
		case 8:
			return otherMode.w1; // Lo
		case 9:
			return otherMode.w0; // Hi
		case 11:
			glDrawTriangles();
			return 0;
		case 13: // swap buffers
			if (colorbufferChanged)
			{
				hDC.swapBuffers();
				colorbufferChanged = false;
				return 1;
			}
			return 0;
		default:
			return 0;
		}
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch ((reg - 0x04100000) >> 2)
		{
		case 0:
			regDPC[DPC_START_REG] = value;
			regDPC[DPC_CURRENT_REG] = value;
			break;
		case 1:
			regDPC[DPC_END_REG] = value;
			break;
		case 3:
			if ((value & DPC_CLR_XBUS_DMEM_DMA) != 0)
			{
				regDPC[DPC_STATUS_REG] &= ~DPC_STATUS_XBUS_DMEM_DMA;
			}
			if ((value & DPC_SET_XBUS_DMEM_DMA) != 0)
			{
				regDPC[DPC_STATUS_REG] |= DPC_STATUS_XBUS_DMEM_DMA;
			}
			if ((value & DPC_CLR_FREEZE) != 0)
			{
				regDPC[DPC_STATUS_REG] &= ~DPC_STATUS_FREEZE;
			}
			if ((value & DPC_SET_FREEZE) != 0)
			{
				regDPC[DPC_STATUS_REG] |= DPC_STATUS_FREEZE;
			}
			if ((value & DPC_CLR_FLUSH) != 0)
			{
				regDPC[DPC_STATUS_REG] &= ~DPC_STATUS_FLUSH;
			}
			if ((value & DPC_SET_FLUSH) != 0)
			{
				regDPC[DPC_STATUS_REG] |= DPC_STATUS_FLUSH;
			}
			break;
		case 8:
			if (((value) & 0x3) != ((otherMode.w1) & 0x3))
				changed |= CHANGED_ALPHACOMPARE;
			changed |= CHANGED_RENDERMODE;
			otherMode.w1 = value;
			break;
		case 9:
			if (((value >> 20) & 0x3) != ((otherMode.w0 >> 20) & 0x3))
				changed |= CHANGED_CYCLETYPE;
			otherMode.w0 = value;
			break;
		case 10: // viewport
			vTrans = rdram.read16bit(value + 14) * FIXED2FLOATRECIP10;
			vScale = rdram.read16bit(value + 6) * FIXED2FLOATRECIP10;
			nearZ = vTrans - vScale;
			changed |= CHANGED_VIEWPORT;
			break;
		case 11: // init gl
			if (canvas.isDisplayable())
			{
				if (!render)
				{
					if (hDC == null)
					{
						hDC = canvas.createContext(null).getGLDrawable();
					}
					hDC.setRealized(true);
					context = hDC.createContext(null);
					hDC.swapBuffers(); // TMP
					initGl();
					render = true;
				}
				context.makeCurrent();
				gl = context.getGL().getGL2();
			}
			else
			{
				context.release();
				context.destroy();
				if (hDC != null)
					hDC.setRealized(false);
				render = false;
			}
			break;
		case 12: // cull face
			if (!render)
				break;
			switch (value)
			{
			case 0:
				gl.glDisable(GL2.GL_CULL_FACE);
				break;
			case 1:
				gl.glEnable(GL2.GL_CULL_FACE);
				gl.glCullFace(GL2.GL_BACK);
				break;
			case 2:
				gl.glEnable(GL2.GL_CULL_FACE);
				gl.glCullFace(GL2.GL_FRONT);
				break;
			}
			break;
		case 13: // fog
			if (!render)
				break;
			// if (value != 0)
			// gl.glEnable(GL.GL_FOG);
			// else
			gl.glDisable(GL2.GL_FOG);
			break;
		case 14: // zbuff depth test
			if (!render)
				break;
			if (value != 0)
				gl.glEnable(GL2.GL_DEPTH_TEST);
			else
				gl.glDisable(GL2.GL_DEPTH_TEST);
			break;
		}
	}

	@Override
	public void readDMA(int pAddr, ByteBuffer dma, int offset, int length)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void writeDMA(int pAddr, ByteBuffer cmdObj, int offset, int length)
	{
		if (!render)
			return;

		byte[] cmd = rdp_cmd_data = cmdObj.array();
		int w1 = ((cmd[0] & 0xFF) << 24) | ((cmd[1] & 0xFF) << 16) | ((cmd[2] & 0xFF) << 8) | (cmd[3] & 0xFF);
		int w2 = ((cmd[4] & 0xFF) << 24) | ((cmd[5] & 0xFF) << 16) | ((cmd[6] & 0xFF) << 8) | (cmd[7] & 0xFF);
		rdp_command_table[cmd[0] & 0x3F].exec(w1, w2);
	}

	// Private Methods /////////////////////////////////////////////////////////

	private void gDPTextureRectangle(int w1, int w2, int w3, int w4)
	{
		cache.setTextureTile(w1, w2, w3, w4, otherMode.w0);

		float ulx = ((w2 >> 12) & 0xFFF) * FIXED2FLOATRECIP2;
		float uly = ((w2 >> 0) & 0xFFF) * FIXED2FLOATRECIP2;
		float lrx = ((w1 >> 12) & 0xFFF) * FIXED2FLOATRECIP2;
		float lry = ((w1 >> 0) & 0xFFF) * FIXED2FLOATRECIP2;
		float dsdx = ((w4 >> 16) & 0xFFFF) * FIXED2FLOATRECIP10;
		float dtdy = ((w4 >> 0) & 0xFFFF) * FIXED2FLOATRECIP10;
		float s = ((w3 >> 16) & 0xFFFF) * FIXED2FLOATRECIP5;
		float t = ((w3 >> 0) & 0xFFFF) * FIXED2FLOATRECIP5;

		if (RDP_GETOM_CYCLE_TYPE(otherMode) == G_CYC_COPY)
		{
			dsdx = 1.0f;
			lrx += 1.0f;
			lry += 1.0f;
		}

		float lrs = s + (lrx - ulx - 1) * dsdx;
		float lrt = t + (lry - uly - 1) * dtdy;

		if (lrs > s)
		{
			if (lrt > t)
				glDrawTexturedRect(ulx, uly, lrx, lry, s, t, lrs, lrt, false, RDP_GETOM_Z_SOURCE_SEL(otherMode), RDP_GETOM_CYCLE_TYPE(otherMode));
			else
				glDrawTexturedRect(ulx, lry, lrx, uly, s, lrt, lrs, t, false, RDP_GETOM_Z_SOURCE_SEL(otherMode), RDP_GETOM_CYCLE_TYPE(otherMode));
		}
		else
		{
			if (lrt > t)
				glDrawTexturedRect(lrx, uly, ulx, lry, lrs, t, s, lrt, false, RDP_GETOM_Z_SOURCE_SEL(otherMode), RDP_GETOM_CYCLE_TYPE(otherMode));
			else
				glDrawTexturedRect(lrx, lry, ulx, uly, lrs, lrt, s, t, false, RDP_GETOM_Z_SOURCE_SEL(otherMode), RDP_GETOM_CYCLE_TYPE(otherMode));
		}

		cache.resetTextureTile();

		if (current != null)
			current.cleared = false;
		// colorImage.changed = true;
		colorImage.height = (int) StrictMath.max(colorImage.height, scissor.lry);
	}

	private void gDPTextureRectangleFlip(int w1, int w2, int w3, int w4)
	{
		cache.setTextureTileFlip(w1, w2, w3, w4, otherMode.w0);

		float ulx = ((w2 >> 12) & 0xFFF) * FIXED2FLOATRECIP2;
		float uly = ((w2 >> 0) & 0xFFF) * FIXED2FLOATRECIP2;
		float lrx = ((w1 >> 12) & 0xFFF) * FIXED2FLOATRECIP2;
		float lry = ((w1 >> 0) & 0xFFF) * FIXED2FLOATRECIP2;
		float dsdx = -(((w4 >> 16) & 0xFFFF) * FIXED2FLOATRECIP10);
		float dtdy = -(((w4 >> 0) & 0xFFFF) * FIXED2FLOATRECIP10);
		float s = (((w3 >> 16) & 0xFFFF) * FIXED2FLOATRECIP5) + (lrx - ulx) * dsdx;
		float t = (((w3 >> 0) & 0xFFFF) * FIXED2FLOATRECIP5) + (lry - uly) * dtdy;

		if (RDP_GETOM_CYCLE_TYPE(otherMode) == G_CYC_COPY)
		{
			dsdx = 1.0f;
			lrx += 1.0f;
			lry += 1.0f;
		}

		float lrs = s + (lrx - ulx - 1) * dsdx;
		float lrt = t + (lry - uly - 1) * dtdy;

		if (lrs > s)
		{
			if (lrt > t)
				glDrawTexturedRect(ulx, uly, lrx, lry, s, t, lrs, lrt, true, RDP_GETOM_Z_SOURCE_SEL(otherMode), RDP_GETOM_CYCLE_TYPE(otherMode));
			else
				glDrawTexturedRect(ulx, lry, lrx, uly, s, lrt, lrs, t, true, RDP_GETOM_Z_SOURCE_SEL(otherMode), RDP_GETOM_CYCLE_TYPE(otherMode));
		}
		else
		{
			if (lrt > t)
				glDrawTexturedRect(lrx, uly, ulx, lry, lrs, t, s, lrt, true, RDP_GETOM_Z_SOURCE_SEL(otherMode), RDP_GETOM_CYCLE_TYPE(otherMode));
			else
				glDrawTexturedRect(lrx, lry, ulx, uly, lrs, lrt, s, t, true, RDP_GETOM_Z_SOURCE_SEL(otherMode), RDP_GETOM_CYCLE_TYPE(otherMode));
		}

		cache.resetTextureTile();

		if (current != null)
			current.cleared = false;
		// colorImage.changed = true;
		colorImage.height = (int) StrictMath.max(colorImage.height, scissor.lry);
	}

	private void update()
	{
		if (current != null)
			current.cleared = false;
		// colorImage.changed = true;
		colorImage.height = (int) StrictMath.max(colorImage.height, scissor.lry);
	}

	private void initGl()
	{
		context.makeCurrent();
		gl = context.getGL().getGL2();

		if (debug) System.out.println("GL_NV_register_combiners: " + Combiners.NV_register_combiners);
		Combiners.ARB_multitexture = gl.glGetString(GL2.GL_EXTENSIONS).contains("GL_ARB_multitexture");
		if (debug) System.out.println("GL_ARB_multitexture: " + Combiners.ARB_multitexture);
		if (Combiners.ARB_multitexture)
		{
			int[] maxTextureUnits_t = new int[1];
			gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_UNITS, maxTextureUnits_t, 0);
			Combiners.maxTextureUnits = StrictMath.min(8, maxTextureUnits_t[0]); // The plugin only supports 8, and 4 is really enough
		}
		Combiners.EXT_fog_coord = gl.glGetString(GL2.GL_EXTENSIONS).contains("GL_EXT_fog_coord");
		if (debug) System.out.println("GL_EXT_fog_coord: " + Combiners.EXT_fog_coord);
		Combiners.EXT_secondary_color = gl.glGetString(GL2.GL_EXTENSIONS).contains("GL_EXT_secondary_color");
		if (debug) System.out.println("GL_EXT_secondary_color: " + Combiners.EXT_secondary_color);
		Combiners.ARB_texture_env_combine = gl.glGetString(GL2.GL_EXTENSIONS).contains("GL_ARB_texture_env_combine");
		if (debug) System.out.println("GL_ARB_texture_env_combine: " + Combiners.ARB_texture_env_combine);
		Combiners.ARB_texture_env_crossbar = gl.glGetString(GL2.GL_EXTENSIONS).contains("GL_ARB_texture_env_crossbar");
		if (debug) System.out.println("GL_ARB_texture_env_crossbar: " + Combiners.ARB_texture_env_crossbar);
		Combiners.EXT_texture_env_combine = gl.glGetString(GL2.GL_EXTENSIONS).contains("GL_EXT_texture_env_combine");
		if (debug) System.out.println("GL_EXT_texture_env_combine: " + Combiners.EXT_texture_env_combine);
		Combiners.ATI_texture_env_combine3 = gl.glGetString(GL2.GL_EXTENSIONS).contains("GL_ATI_texture_env_combine3");
		if (debug) System.out.println("GL_ATI_texture_env_combine3: " + Combiners.ATI_texture_env_combine3);
		if (debug) System.out.println("GL_ATIX_texture_env_route: " + Combiners.ATIX_texture_env_route);
		if (debug) System.out.println("GL_NV_texture_env_combine4: " + Combiners.NV_texture_env_combine4);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		bigArray.position(0);
		gl.glVertexPointer(4, GL2.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		bigArray.position(4);
		gl.glColorPointer(4, GL2.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
		gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
		if (Combiners.EXT_secondary_color)
		{
			bigArray.position(8);
			gl.glSecondaryColorPointer(3, GL2.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
			gl.glEnableClientState(GL2.GL_SECONDARY_COLOR_ARRAY);
		}
		if (Combiners.ARB_multitexture)
		{
			gl.glClientActiveTexture(GL2.GL_TEXTURE0);
			bigArray.position(12);
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
			gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
			gl.glClientActiveTexture(GL2.GL_TEXTURE1);
			bigArray.position(14);
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
			gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		}
		else
		{
			bigArray.position(12);
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
			gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		}
		if (Combiners.EXT_fog_coord)
		{
			gl.glFogi(GL2.GL_FOG_COORDINATE_SOURCE, GL2.GL_FOG_COORDINATE);
			gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
			gl.glFogf(GL2.GL_FOG_START, 0.0f);
			gl.glFogf(GL2.GL_FOG_END, 255.0f);
			bigArray.position(16);
			gl.glFogCoordPointer(GL2.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
			gl.glEnableClientState(GL2.GL_FOG_COORDINATE_ARRAY);
		}
		gl.glPolygonOffset(-3.0f, -3.0f);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

		cache.init(gl, (Hardware) rdram, tmem, Combiners.maxTextureUnits, Combiners.ARB_multitexture);
		combiners.init();
		updateScale();
		changed = 0xFFFFFFFF;
		context.release();
	}

	private void resetDp()
	{
		updateSize();
		updateScale();

		otherMode.w1 &= ~ALPHA_COMPARE;
		otherMode.w1 |= ((G_AC_NONE & 0x3) << 0);
		otherMode.w1 &= ~DEPTH_SOURCE;
		otherMode.w1 |= ((G_ZS_PIXEL & 0x1) << 2);
		otherMode.w1 &= 0x00000007;
		otherMode.w1 |= 0;

		otherMode.w0 &= ~ALPHA_DITHER;
		otherMode.w0 |= ((G_AD_DISABLE & 0x3) << 4);
		otherMode.w0 &= ~COLOR_DITHER;
		otherMode.w0 |= ((G_CD_DISABLE & 0x3) << 6);
		otherMode.w0 &= ~COMBINE_KEY;
		otherMode.w0 |= ((G_CK_NONE & 0x1) << 8);
		otherMode.w0 &= ~TEX_CONVERT;
		otherMode.w0 |= ((G_TC_FILT & 0x7) << 9);
		otherMode.w0 &= ~TEX_FILTER;
		otherMode.w0 |= ((G_TF_POINT & 0x3) << 12);
		otherMode.w0 &= ~TEX_LUT;
		otherMode.w0 |= ((G_TT_NONE & 0x3) << 14);
		otherMode.w0 &= ~TEX_LOD;
		otherMode.w0 |= ((G_TL_TILE & 0x1) << 16);
		otherMode.w0 &= ~TEX_DETAIL;
		otherMode.w0 |= ((G_TD_CLAMP & 0x3) << 17);
		otherMode.w0 &= ~TEX_PERSP;
		;
		otherMode.w0 |= ((G_TP_PERSP & 0x1) << 19);
		otherMode.w0 &= ~CYCLE_TYPE;
		otherMode.w0 |= ((G_CYC_1CYCLE & 0x3) << 20);
		otherMode.w0 &= ~PIPELINE_MODE;
		otherMode.w0 |= ((G_PM_NPRIMITIVE & 0x1) << 23);
	}

	private void updateScale()
	{
		scaleX = width / (float) screenWidth;
		scaleY = height / (float) screenHeight;
	}

	private void updateSize()
	{
		float xScale = (vi.read32bit(0x04400000 + (VI_X_SCALE_REG << 2)) & 0xFFF) * 0.0009765625f;
		float yScale = (vi.read32bit(0x04400000 + (VI_Y_SCALE_REG << 2)) & 0xFFF) * 0.0009765625f;

		int hEnd = vi.read32bit(0x04400000 + (VI_H_START_REG << 2)) & 0x3FF;
		int hStart = (vi.read32bit(0x04400000 + (VI_H_START_REG << 2)) >> 16) & 0x3FF;

		// These are in half-lines, so shift an extra bit
		int vEnd = (vi.read32bit(0x04400000 + (VI_V_START_REG << 2)) >> 1) & 0x1FF;
		int vStart = (vi.read32bit(0x04400000 + (VI_V_START_REG << 2)) >> 17) & 0x1FF;

		screenWidth = (int) ((hEnd - hStart) * xScale);
		screenHeight = (int) ((vEnd - vStart) * yScale * 1.0126582f);

		if (screenWidth == 0)
			screenWidth = 320;
		if (screenHeight == 0)
			screenHeight = 240;
	}

	private void addTriangle(float[] vtx1, float[] c1, float[] tex1, float[] vtx2, float[] c2, float[] tex2, float[] vtx3, float[] c3, float[] tex3)
	{
		if (changed != 0)
		{
			glUpdateStates();
		}

		final int VTX = 0;
		final int CLR = 1;
		final int TEX = 2;
		float[][][] v = { { vtx1, c1, tex1 }, { vtx2, c2, tex2 }, { vtx3, c3, tex3 } };

		for (int i = 0; i < 3; i++)
		{
			float[][] spvert = v[i];
			GLVertex vertex = vertices[numVertices];
			vertex.vtx.put(0, spvert[VTX][0]);
			vertex.vtx.put(1, spvert[VTX][1]);
			vertex.vtx.put(2, RDP_GETOM_Z_SOURCE_SEL(otherMode) == G_ZS_PRIM ? zDepth * spvert[VTX][3] : spvert[VTX][2]);
			vertex.vtx.put(3, spvert[VTX][3]);

			vertex.color.put(0, spvert[CLR][0]);
			vertex.color.put(1, spvert[CLR][1]);
			vertex.color.put(2, spvert[CLR][2]);
			vertex.color.put(3, spvert[CLR][3]);
			combiners.setConstant(vertex.color, combiners.vertex.color, combiners.vertex.alpha);

			if (Combiners.EXT_secondary_color)
			{
				vertex.secondaryColor.put(0, 0.0f);
				vertex.secondaryColor.put(0, 0.0f);
				vertex.secondaryColor.put(0, 0.0f);
				vertex.secondaryColor.put(0, 1.0f);
				combiners.setConstant(vertex.secondaryColor, combiners.vertex.secondaryColor, Combiners.ONE);
			}

			if (combiners.usesT0)
			{
				vertex.tex0.put(0, cache.getTexS(0, spvert[TEX][0]));
				vertex.tex0.put(1, cache.getTexT(0, spvert[TEX][1]));
			}
			if (combiners.usesT1)
			{
				vertex.tex1.put(0, cache.getTexS(1, spvert[TEX][0]));
				vertex.tex1.put(1, cache.getTexT(1, spvert[TEX][1]));
			}
			numVertices++;
		}

		numTriangles++;

		if (numVertices >= 255)
			glDrawTriangles();
	}

	private void glClearDepthBuffer(boolean depthUpdate)
	{
		glUpdateStates();

		gl.glDepthMask(true);
		gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		gl.glDepthMask(depthUpdate);
	}

	private void glClearColorBuffer(float[] color)
	{
		gl.glClearColor(color[0], color[1], color[2], color[3]);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
	}

	private void glDrawRect(int ulx, int uly, int lrx, int lry, float[] color, int depthSource)
	{
		glUpdateStates();

		boolean culling = gl.glIsEnabled(GL2.GL_CULL_FACE);
		gl.glDisable(GL2.GL_SCISSOR_TEST);
		gl.glDisable(GL2.GL_CULL_FACE);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, screenWidth, screenHeight, 0, 1.0f, -1.0f);
		gl.glViewport(0, heightOffset, width, height);
		if (wireframe)
			gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE); // TMP
		gl.glDepthRange(0.0f, 1.0f);
		gl.glColor4f(color[0], color[1], color[2], color[3]);

		gl.glBegin(GL2.GL_QUADS);
		gl.glVertex4f(ulx, uly, (depthSource == G_ZS_PRIM) ? zDepth : nearZ, 1.0f);
		gl.glVertex4f(lrx, uly, (depthSource == G_ZS_PRIM) ? zDepth : nearZ, 1.0f);
		gl.glVertex4f(lrx, lry, (depthSource == G_ZS_PRIM) ? zDepth : nearZ, 1.0f);
		gl.glVertex4f(ulx, lry, (depthSource == G_ZS_PRIM) ? zDepth : nearZ, 1.0f);
		gl.glEnd();

		gl.glLoadIdentity();

		if (culling)
			gl.glEnable(GL2.GL_CULL_FACE);

		gl.glEnable(GL2.GL_SCISSOR_TEST);
	}

	private void glDrawTexturedRect(float ulx, float uly, float lrx, float lry, float uls, float ult, float lrs, float lrt, boolean flip, int depthSource, int cycleType)
	{
		glUpdateStates();

		rect0.x = ulx;
		rect0.y = uly;
		rect0.z = depthSource == G_ZS_PRIM ? zDepth : nearZ;
		rect0.w = 1.0f;
		rect0.color[0] = 1.0f;
		rect0.color[1] = 1.0f;
		rect0.color[2] = 1.0f;
		rect0.color[3] = 0.0f;
		rect0.secondaryColor[0] = 1.0f;
		rect0.secondaryColor[1] = 1.0f;
		rect0.secondaryColor[2] = 1.0f;
		rect0.secondaryColor[3] = 1.0f;
		rect0.s0 = uls;
		rect0.t0 = ult;
		rect0.s1 = uls;
		rect0.t1 = ult;
		rect0.fog = 0.0f;

		rect1.x = lrx;
		rect1.y = lry;
		rect1.z = depthSource == G_ZS_PRIM ? zDepth : nearZ;
		rect1.w = 1.0f;
		rect1.color[0] = 1.0f;
		rect1.color[1] = 1.0f;
		rect1.color[2] = 1.0f;
		rect1.color[3] = 0.0f;
		rect1.secondaryColor[0] = 1.0f;
		rect1.secondaryColor[1] = 1.0f;
		rect1.secondaryColor[2] = 1.0f;
		rect1.secondaryColor[3] = 1.0f;
		rect1.s0 = lrs;
		rect1.t0 = lrt;
		rect1.s1 = lrs;
		rect1.t1 = lrt;
		rect1.fog = 0.0f;

		boolean culling = gl.glIsEnabled(GL2.GL_CULL_FACE);
		gl.glDisable(GL2.GL_CULL_FACE);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, screenWidth, screenHeight, 0, 1.0f, -1.0f); // left, right, bottom, top, near, far
		gl.glViewport(0, heightOffset, width, height); // x, y, width, height
		if (wireframe)
			gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE); // TMP

		if (combiners.usesT0)
		{
			rect0.s0 = rect0.s0 * cache.current[0].shiftScaleS - cache.textureTile[0].fuls;
			rect0.t0 = rect0.t0 * cache.current[0].shiftScaleT - cache.textureTile[0].fult;
			rect1.s0 = (rect1.s0 + 1.0f) * cache.current[0].shiftScaleS - cache.textureTile[0].fuls;
			rect1.t0 = (rect1.t0 + 1.0f) * cache.current[0].shiftScaleT - cache.textureTile[0].fult;

			if ((cache.current[0].maskS != 0) && (StrictMath.IEEEremainder(rect0.s0, cache.current[0].width) == 0.0f) && (cache.current[0].mirrorS == 0))
			{
				rect1.s0 -= rect0.s0;
				rect0.s0 = 0.0f;
			}
			if ((cache.current[0].maskT != 0) && (StrictMath.IEEEremainder(rect0.t0, cache.current[0].height) == 0.0f) && (cache.current[0].mirrorT == 0))
			{
				rect1.t0 -= rect0.t0;
				rect0.t0 = 0.0f;
			}

			if (Combiners.ARB_multitexture)
				gl.glActiveTexture(GL2.GL_TEXTURE0);

			if ((rect0.s0 >= 0.0f) && (rect1.s0 <= cache.current[0].width))
			{
				gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
			}
			if ((rect0.t0 >= 0.0f) && (rect1.t0 <= cache.current[0].height))
			{
				gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
			}

			rect0.s0 *= cache.current[0].scaleS;
			rect0.t0 *= cache.current[0].scaleT;
			rect1.s0 *= cache.current[0].scaleS;
			rect1.t0 *= cache.current[0].scaleT;
		}

		if (combiners.usesT1 && Combiners.ARB_multitexture)
		{
			rect0.s1 = rect0.s1 * cache.current[1].shiftScaleS - cache.textureTile[1].fuls;
			rect0.t1 = rect0.t1 * cache.current[1].shiftScaleT - cache.textureTile[1].fult;
			rect1.s1 = (rect1.s1 + 1.0f) * cache.current[1].shiftScaleS - cache.textureTile[1].fuls;
			rect1.t1 = (rect1.t1 + 1.0f) * cache.current[1].shiftScaleT - cache.textureTile[1].fult;

			if ((cache.current[1].maskS != 0) && (StrictMath.IEEEremainder(rect0.s1, cache.current[1].width) == 0.0f) && (cache.current[1].mirrorS == 0))
			{
				rect1.s1 -= rect0.s1;
				rect0.s1 = 0.0f;
			}
			if ((cache.current[1].maskT != 0) && (StrictMath.IEEEremainder(rect0.t1, cache.current[1].height) == 0.0f) && (cache.current[1].mirrorT == 0))
			{
				rect1.t1 -= rect0.t1;
				rect0.t1 = 0.0f;
			}

			gl.glActiveTexture(GL2.GL_TEXTURE1);

			if ((rect0.s1 == 0.0f) && (rect1.s1 <= cache.current[1].width))
			{
				gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
			}
			if ((rect0.t1 == 0.0f) && (rect1.t1 <= cache.current[1].height))
			{
				gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
			}

			rect0.s1 *= cache.current[1].scaleS;
			rect0.t1 *= cache.current[1].scaleT;
			rect1.s1 *= cache.current[1].scaleS;
			rect1.t1 *= cache.current[1].scaleT;
		}

		if (cycleType == G_CYC_COPY)
		{
			if (Combiners.ARB_multitexture)
				gl.glActiveTexture(GL2.GL_TEXTURE0);

			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
		}

		combiners.setConstant(rect0.color, combiners.vertex.color, combiners.vertex.alpha);

		if (Combiners.EXT_secondary_color)
			combiners.setConstant(rect0.secondaryColor, combiners.vertex.secondaryColor, combiners.vertex.alpha);

		gl.glBegin(GL2.GL_QUADS);
		gl.glColor4f(rect0.color[0], rect0.color[1], rect0.color[2], rect0.color[3]);
		if (Combiners.EXT_secondary_color)
			gl.glSecondaryColor3f(rect0.secondaryColor[0], rect0.secondaryColor[1], rect0.secondaryColor[2]);

		if (Combiners.ARB_multitexture)
		{
			gl.glMultiTexCoord2f(GL2.GL_TEXTURE0, rect0.s0, rect0.t0);
			gl.glMultiTexCoord2f(GL2.GL_TEXTURE1, rect0.s1, rect0.t1);
			gl.glVertex4f(rect0.x, rect0.y, rect0.z, 1.0f);

			gl.glMultiTexCoord2f(GL2.GL_TEXTURE0, rect1.s0, rect0.t0);
			gl.glMultiTexCoord2f(GL2.GL_TEXTURE1, rect1.s1, rect0.t1);
			gl.glVertex4f(rect1.x, rect0.y, rect0.z, 1.0f);

			gl.glMultiTexCoord2f(GL2.GL_TEXTURE0, rect1.s0, rect1.t0);
			gl.glMultiTexCoord2f(GL2.GL_TEXTURE1, rect1.s1, rect1.t1);
			gl.glVertex4f(rect1.x, rect1.y, rect0.z, 1.0f);

			gl.glMultiTexCoord2f(GL2.GL_TEXTURE0, rect0.s0, rect1.t0);
			gl.glMultiTexCoord2f(GL2.GL_TEXTURE1, rect0.s1, rect1.t1);
			gl.glVertex4f(rect0.x, rect1.y, rect0.z, 1.0f);
		}
		else
		{
			gl.glTexCoord2f(rect0.s0, rect0.t0);
			gl.glVertex4f(rect0.x, rect0.y, rect0.z, 1.0f);

			if (flip)
				gl.glTexCoord2f(rect0.s0, rect1.t0);
			else
				gl.glTexCoord2f(rect1.s0, rect0.t0);
			gl.glVertex4f(rect1.x, rect0.y, rect0.z, 1.0f);

			gl.glTexCoord2f(rect1.s0, rect1.t0);
			gl.glVertex4f(rect1.x, rect1.y, rect0.z, 1.0f);

			if (flip)
				gl.glTexCoord2f(rect1.s0, rect0.t0);
			else
				gl.glTexCoord2f(rect0.s0, rect1.t0);
			gl.glVertex4f(rect0.x, rect1.y, rect0.z, 1.0f);
		}
		gl.glEnd();

		gl.glLoadIdentity();

		if (culling)
			gl.glEnable(GL2.GL_CULL_FACE);
	}

	private void glDrawLine(float[] vtx1, float[] c1, float[] vtx2, float[] c2, float width)
	{
		if (changed != 0)
		{
			glUpdateStates();
		}

		final int VTX = 0;
		final int CLR = 1;
		float[][][] v = { { vtx1, c1 }, { vtx2, c2 } };

		float[] color = new float[4];

		gl.glLineWidth(width * scaleX);

		gl.glBegin(GL2.GL_LINES);
		for (int i = 0; i < 2; i++)
		{
			float[][] spvert = v[i];
			color[0] = spvert[CLR][0];
			color[1] = spvert[CLR][1];
			color[2] = spvert[CLR][2];
			color[3] = spvert[CLR][3];
			combiners.setConstant(color, combiners.vertex.color, combiners.vertex.alpha);
			gl.glColor4fv(color, 0);

			if (Combiners.EXT_secondary_color)
			{
				color[0] = spvert[CLR][0];
				color[1] = spvert[CLR][1];
				color[2] = spvert[CLR][2];
				color[3] = spvert[CLR][3];
				combiners.setConstant(color, combiners.vertex.secondaryColor, combiners.vertex.alpha);
				gl.glSecondaryColor3fv(color, 0);
			}

			gl.glVertex4f(spvert[VTX][0], spvert[VTX][1], spvert[VTX][2], spvert[VTX][3]);
		}
		gl.glEnd();
	}

	private void glDrawTriangles()
	{
		if (numTriangles < 1)
			return;
		glStipple();
		gl.glDrawArrays(GL2.GL_TRIANGLES, 0, numVertices);
		numTriangles = numVertices = 0;
	}

	private void glStipple()
	{
		if (usePolygonStipple && (RDP_GETOM_ALPHA_COMPARE_EN(otherMode) == G_AC_DITHER) && (RDP_GETOM_ALPHA_CVG_SELECT(otherMode) == 0))
		{
			lastStipple = (lastStipple + 1) & 0x7;
			gl.glPolygonStipple(stipplePattern[(int) (Combiners.envColor[3] * 255.0f) >> 3][lastStipple], 0);
		}
	}

	private void glUpdateStates()
	{
		if ((changed & CHANGED_RENDERMODE) != 0)
		{
			if (RDP_GETOM_Z_COMPARE_EN(otherMode) != 0)
				gl.glDepthFunc(GL2.GL_LEQUAL);
			else
				gl.glDepthFunc(GL2.GL_ALWAYS);

			if (RDP_GETOM_Z_UPDATE_EN(otherMode) != 0)
				gl.glDepthMask(true);
			else
				gl.glDepthMask(false);

			if (RDP_GETOM_Z_MODE(otherMode) == ZMODE_DEC)
				gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
			else
			{
				gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
			}
		}

		if ((changed & CHANGED_ALPHACOMPARE) != 0 || (changed & CHANGED_RENDERMODE) != 0)
		{
			// Enable alpha test for threshold mode
			if ((RDP_GETOM_ALPHA_COMPARE_EN(otherMode) == G_AC_THRESHOLD) && (RDP_GETOM_ALPHA_CVG_SELECT(otherMode) == 0))
			{
				gl.glEnable(GL2.GL_ALPHA_TEST);

				gl.glAlphaFunc((blendColor[3] > 0.0f) ? GL2.GL_GEQUAL : GL2.GL_GREATER, blendColor[3]);
			}
			// Used in TEX_EDGE and similar render modes
			else if (RDP_GETOM_CVG_TIMES_ALPHA(otherMode) != 0)
			{
				gl.glEnable(GL2.GL_ALPHA_TEST);

				// Arbitrary number -- gives nice results though
				gl.glAlphaFunc(GL2.GL_GEQUAL, 0.5f);
			}
			else
			{
				gl.glDisable(GL2.GL_ALPHA_TEST);
			}

			if (usePolygonStipple && (RDP_GETOM_ALPHA_COMPARE_EN(otherMode) == G_AC_DITHER) && (RDP_GETOM_ALPHA_CVG_SELECT(otherMode) == 0))
				gl.glEnable(GL2.GL_POLYGON_STIPPLE);
			else
				gl.glDisable(GL2.GL_POLYGON_STIPPLE);
		}

		if ((changed & CHANGED_SCISSOR) != 0)
		{
			gl.glScissor((int) (scissor.ulx * scaleX), (int) ((screenHeight - scissor.lry) * scaleY + heightOffset),
					(int) ((scissor.lrx - scissor.ulx) * scaleX), (int) ((scissor.lry - scissor.uly) * scaleY));
		}

		if ((changed & CHANGED_COMBINE) != 0 || (changed & CHANGED_CYCLETYPE) != 0)
		{
			if (RDP_GETOM_CYCLE_TYPE(otherMode) == G_CYC_COPY)
			{
				combiners.setCombine(gl, false, combiners.encodeCombineMode(
						Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_TEXEL0,
						Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_TEXEL0,
						Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_TEXEL0,
						Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_TEXEL0));
			}
			else if (RDP_GETOM_CYCLE_TYPE(otherMode) == G_CYC_FILL)
			{
				combiners.setCombine(gl, false, combiners.encodeCombineMode(
						Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_SHADE,
						Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_1,
						Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_SHADE,
						Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_1));
			}
			else
			{
				combiners.setCombine(gl, RDP_GETOM_CYCLE_TYPE(otherMode) == G_CYC_2CYCLE, combiners.combine.getMux());
			}
			changed |= CHANGED_COMBINE_COLORS;
		}

		if ((changed & CHANGED_COMBINE_COLORS) != 0)
		{
			combiners.updateCombineColors(gl);
			changed &= ~CHANGED_COMBINE_COLORS;
		}

		if ((changed & CHANGED_TEXTURE) != 0 || (changed & CHANGED_TILE) != 0 || (cache.changed & TextureCache.CHANGED_TMEM) != 0)
		{
			combiners.beginTextureUpdate(gl);

			if (combiners.usesT0)
			{
				cache.update(0, otherMode.w0);

				changed &= ~CHANGED_TEXTURE;
				changed &= ~CHANGED_TILE;
				cache.changed &= ~TextureCache.CHANGED_TMEM;
			}
			else
			{
				cache.activateDummy(0);
			}

			// NOTE: The following block does not appear to work if
			// combiner uses T0 but not T1
			if (combiners.usesT1)
			{
				cache.update(1, otherMode.w0);

				changed &= ~CHANGED_TEXTURE;
				changed &= ~CHANGED_TILE;
				cache.changed &= ~TextureCache.CHANGED_TMEM;
			}
			else
			{
				cache.activateDummy(1);
			}

			combiners.endTextureUpdate(gl);
		}

		if ((changed & CHANGED_RENDERMODE) != 0 || (changed & CHANGED_CYCLETYPE) != 0)
		{
			if ((RDP_GETOM_FORCE_BLEND(otherMode) != 0) &&
					(RDP_GETOM_CYCLE_TYPE(otherMode) != G_CYC_COPY) &&
					(RDP_GETOM_CYCLE_TYPE(otherMode) != G_CYC_FILL) &&
					(RDP_GETOM_ALPHA_CVG_SELECT(otherMode) == 0))
			{
				gl.glEnable(GL2.GL_BLEND);

				switch (otherMode.w1 >> 16)
				{
				case 0x0448: // Add
				case 0x055A:
					gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE);
					break;
				case 0x0C08: // 1080 Sky
				case 0x0F0A: // Used LOTS of places
					gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ZERO);
					break;
				case 0xC810: // Blends fog
				case 0xC811: // Blends fog
				case 0x0C18: // Standard interpolated blend
				case 0x0C19: // Used for antialiasing
				case 0x0050: // Standard interpolated blend
				case 0x0055: // Used for antialiasing
					gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
					break;
				case 0x0FA5: // Seems to be doing just blend color - maybe combiner can be used for this?
				case 0x5055: // Used in Paper Mario intro, I'm not sure if this is right...
					gl.glBlendFunc(GL2.GL_ZERO, GL2.GL_ONE);
					break;
				default:
					gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
					break;
				}
			}
			else
			{
				gl.glDisable(GL2.GL_BLEND);
			}

			if (RDP_GETOM_CYCLE_TYPE(otherMode) == G_CYC_FILL)
			{
				gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
				gl.glEnable(GL2.GL_BLEND);
			}
		}

		cache.changed &= TextureCache.CHANGED_TMEM;
		changed &= CHANGED_TILE;
		changed &= CHANGED_TEXTURE;
	}

	private void buildOps()
	{
		rdp_command_table = new OpCode[64];
		for (int i = 0; i < 64; i++)
			rdp_command_table[i] = rdp_invalid;
		rdp_command_table[0] = rdp_noop;
		rdp_command_table[8] = rdp_tri_noshade;
		rdp_command_table[9] = rdp_tri_noshade_z;
		rdp_command_table[10] = rdp_tri_tex;
		rdp_command_table[11] = rdp_tri_tex_z;
		rdp_command_table[12] = rdp_tri_shade;
		rdp_command_table[13] = rdp_tri_shade_z;
		rdp_command_table[14] = rdp_tri_texshade;
		rdp_command_table[15] = rdp_tri_texshade_z;

		rdp_command_table[16] = rdp_hle_triangle; // rdp_invalid
		rdp_command_table[17] = rdp_hle_line; // rdp_invalid
		rdp_command_table[18] = rdp_hle_texture; // rdp_invalid

		rdp_command_table[36] = rdp_tex_rect;
		rdp_command_table[37] = rdp_tex_rect_flip;
		rdp_command_table[38] = rdp_sync_load;
		rdp_command_table[39] = rdp_sync_pipe;
		rdp_command_table[40] = rdp_sync_tile;
		rdp_command_table[41] = rdp_sync_full;
		rdp_command_table[42] = rdp_set_key_gb;
		rdp_command_table[43] = rdp_set_key_r;
		rdp_command_table[44] = rdp_set_convert;
		rdp_command_table[45] = rdp_set_scissor;
		rdp_command_table[46] = rdp_set_prim_depth;
		rdp_command_table[47] = rdp_set_other_modes;
		rdp_command_table[48] = rdp_load_tlut;
		rdp_command_table[49] = rdp_invalid;
		rdp_command_table[50] = rdp_set_tile_size;
		rdp_command_table[51] = rdp_load_block;
		rdp_command_table[52] = rdp_load_tile;
		rdp_command_table[53] = rdp_set_tile;
		rdp_command_table[54] = rdp_fill_rect;
		rdp_command_table[55] = rdp_set_fill_color;
		rdp_command_table[56] = rdp_set_fog_color;
		rdp_command_table[57] = rdp_set_blend_color;
		rdp_command_table[58] = rdp_set_prim_color;
		rdp_command_table[59] = rdp_set_env_color;
		rdp_command_table[60] = rdp_set_combine;
		rdp_command_table[61] = rdp_set_texture_image;
		rdp_command_table[62] = rdp_set_mask_image;
		rdp_command_table[63] = rdp_set_color_image;
	}

	/************************* OpCode functions *************************/

	protected OpCode rdp_noop = new OpCode()
	{
		public void exec(int w1, int w2)
		{
		}
	};

	protected OpCode rdp_tri_noshade = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			rglTriangle(w1, w2, 0, 0, 0, rdp_cmd_data, 0);
			update();
		}
	};

	protected OpCode rdp_tri_noshade_z = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			rglTriangle(w1, w2, 0, 0, 1, rdp_cmd_data, 0);
			update();
		}
	};

	protected OpCode rdp_tri_tex = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			rglTriangle(w1, w2, 0, 1, 0, rdp_cmd_data, 0);
			update();
		}
	};

	protected OpCode rdp_tri_tex_z = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			rglTriangle(w1, w2, 0, 1, 1, rdp_cmd_data, 0);
			update();
		}
	};

	protected OpCode rdp_tri_shade = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			rglTriangle(w1, w2, 1, 0, 0, rdp_cmd_data, 0);
			update();
		}
	};

	protected OpCode rdp_tri_shade_z = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			rglTriangle(w1, w2, 1, 0, 1, rdp_cmd_data, 0);
			update();
		}
	};

	protected OpCode rdp_tri_texshade = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			rglTriangle(w1, w2, 1, 1, 0, rdp_cmd_data, 0);
			update();
		}
	};

	protected OpCode rdp_tri_texshade_z = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			rglTriangle(w1, w2, 1, 1, 1, rdp_cmd_data, 0);
			update();
		}
	};

	protected OpCode rdp_tex_rect = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			int w3 = ((rdp_cmd_data[8] & 0xFF) << 24) | ((rdp_cmd_data[9] & 0xFF) << 16) | ((rdp_cmd_data[10] & 0xFF) << 8) | (rdp_cmd_data[11] & 0xFF);
			int w4 = ((rdp_cmd_data[12] & 0xFF) << 24) | ((rdp_cmd_data[13] & 0xFF) << 16) | ((rdp_cmd_data[14] & 0xFF) << 8) | (rdp_cmd_data[15] & 0xFF);
			gDPTextureRectangle(w1, w2, w3, w4);
		}
	};

	protected OpCode rdp_tex_rect_flip = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			int w3 = ((rdp_cmd_data[8] & 0xFF) << 24) | ((rdp_cmd_data[9] & 0xFF) << 16) | ((rdp_cmd_data[10] & 0xFF) << 8) | (rdp_cmd_data[11] & 0xFF);
			int w4 = ((rdp_cmd_data[12] & 0xFF) << 24) | ((rdp_cmd_data[13] & 0xFF) << 16) | ((rdp_cmd_data[14] & 0xFF) << 8) | (rdp_cmd_data[15] & 0xFF);
			gDPTextureRectangleFlip(w1, w2, w3, w4);
		}
	};

	protected OpCode rdp_sync_load = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			// Nothing to do?
		}
	};

	protected OpCode rdp_sync_pipe = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			// Nothing to do?
		}
	};

	protected OpCode rdp_sync_tile = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			// Nothing to do?
		}
	};

	protected OpCode rdp_sync_full = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			mi.write32bit(MI_INTR_REG, MI_INTR_SET_DP);
		}
	};

	protected OpCode rdp_set_key_gb = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			// Nothing to do?
		}
	};

	protected OpCode rdp_set_key_r = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			// Nothing to do?
		}
	};

	protected OpCode rdp_set_convert = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			// Nothing to do?
		}
	};

	protected OpCode rdp_set_scissor = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			// scissor.mode = (w2 >> 24) & 0x3;
			scissor.ulx = ((w1 >> 12) & 0xFFF) * FIXED2FLOATRECIP2;
			scissor.uly = ((w1 >> 0) & 0xFFF) * FIXED2FLOATRECIP2;
			scissor.lrx = ((w2 >> 12) & 0xFFF) * FIXED2FLOATRECIP2;
			scissor.lry = ((w2 >> 0) & 0xFFF) * FIXED2FLOATRECIP2;
			changed |= CHANGED_SCISSOR;
		}
	};

	protected OpCode rdp_set_prim_depth = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			primDepth.z = (w2 >> 16) & 0xFFFF;
			// primDepth.deltaZ = w2 & 0xFFFF;
			zDepth = StrictMath.min(1.0f, StrictMath.max(0.0f, ((primDepth.z * 3.0517578e-05f) - vTrans) / vScale));
		}
	};

	protected OpCode rdp_set_other_modes = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			otherMode.w0 = w1 & 0xFFFFFF;
			otherMode.w1 = w2;
			changed |= CHANGED_RENDERMODE | CHANGED_CYCLETYPE | CHANGED_ALPHACOMPARE;
		}
	};

	protected OpCode rdp_load_tlut = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cache.setTileSize(w1, w2);
			changed |= CHANGED_TILE;
			cache.loadLUT(w1, w2);
		}
	};

	protected OpCode rdp_set_tile_size = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cache.setTileSize(w1, w2);
			changed |= CHANGED_TILE;
		}
	};

	protected OpCode rdp_load_block = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cache.setTileSize(w1, w2);
			changed |= CHANGED_TILE;
			cache.loadBlock(w1, w2);
		}
	};

	protected OpCode rdp_load_tile = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cache.setTileSize(w1, w2);
			changed |= CHANGED_TILE;
			cache.loadTile(w1, w2);
		}
	};

	protected OpCode rdp_set_tile = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cache.setTile(w1, w2);
		}
	};

	protected OpCode rdp_fill_rect = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			int ulx = (w2 >> 14) & 0x3FF;
			int uly = (w2 >> 2) & 0x3FF;
			int lrx = (w1 >> 14) & 0x3FF;
			int lry = (w1 >> 2) & 0x3FF;
			DepthBufferStack.DepthBuffer buffer = depthBuffers.findBuffer(colorImage.address);
			if (buffer != null)
				buffer.cleared = true;
			if (depthImageAddress == colorImage.address)
			{
				glClearDepthBuffer(RDP_GETOM_Z_UPDATE_EN(otherMode) != 0);
				return;
			}
			if (RDP_GETOM_CYCLE_TYPE(otherMode) == G_CYC_FILL)
			{
				lrx++;
				lry++;
				if ((ulx == 0) && (uly == 0) && (lrx == screenWidth) && (lry == screenHeight))
				{
					glClearColorBuffer(fillColor.color);
					return;
				}
			}
			glDrawRect(ulx, uly, lrx, lry, (RDP_GETOM_CYCLE_TYPE(otherMode) == G_CYC_FILL) ? fillColor.color : blendColor, RDP_GETOM_Z_SOURCE_SEL(otherMode));
			if (current != null)
				current.cleared = false;
			// colorImage.changed = true;
			colorImage.height = StrictMath.max(colorImage.height, lry);
		}
	};

	protected OpCode rdp_set_fill_color = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			fillColor.color[0] = ((w2 >> 11) & 0x1F) * 0.032258064f;
			fillColor.color[1] = ((w2 >> 6) & 0x1F) * 0.032258064f;
			fillColor.color[2] = ((w2 >> 1) & 0x1F) * 0.032258064f;
			fillColor.color[3] = w2 & 0x1;
			// fillColor.z = (w2 >> 2) & 0x3FFF;
			// fillColor.dz = w2 & 0x3;
		}
	};

	protected OpCode rdp_set_fog_color = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			fogColor[0] = ((w2 >> 24) & 0xFF) * 0.0039215689f; // r / 255
			fogColor[1] = ((w2 >> 16) & 0xFF) * 0.0039215689f; // g / 255
			fogColor[2] = ((w2 >> 8) & 0xFF) * 0.0039215689f; // b / 255
			fogColor[3] = ((w2 >> 0) & 0xFF) * 0.0039215689f; // a / 255
			changed |= CHANGED_FOGCOLOR;
		}
	};

	protected OpCode rdp_set_blend_color = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			blendColor[0] = ((w2 >> 24) & 0xFF) * 0.0039215689f; // r / 255
			blendColor[1] = ((w2 >> 16) & 0xFF) * 0.0039215689f; // g / 255
			blendColor[2] = ((w2 >> 8) & 0xFF) * 0.0039215689f; // b / 255
			blendColor[3] = ((w2 >> 0) & 0xFF) * 0.0039215689f; // a / 255
		}
	};

	protected OpCode rdp_set_prim_color = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			Combiners.primColor.m = (w1 >> 8) & 0x1F;
			Combiners.primColor.l = ((w1 >> 0) & 0xFF) * 0.0039215689f; // l / 255
			Combiners.primColor.r = ((w2 >> 24) & 0xFF) * 0.0039215689f; // r / 255
			Combiners.primColor.g = ((w2 >> 16) & 0xFF) * 0.0039215689f; // g / 255
			Combiners.primColor.b = ((w2 >> 8) & 0xFF) * 0.0039215689f; // b / 255
			Combiners.primColor.a = ((w2 >> 0) & 0xFF) * 0.0039215689f; // a / 255
			changed |= CHANGED_COMBINE_COLORS;
		}
	};

	protected OpCode rdp_set_env_color = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			Combiners.envColor[0] = ((w2 >> 24) & 0xFF) * 0.0039215689f; // r / 255
			Combiners.envColor[1] = ((w2 >> 16) & 0xFF) * 0.0039215689f; // g / 255
			Combiners.envColor[2] = ((w2 >> 8) & 0xFF) * 0.0039215689f; // b / 255
			Combiners.envColor[3] = ((w2 >> 0) & 0xFF) * 0.0039215689f; // a / 255
			changed |= CHANGED_COMBINE_COLORS;
		}
	};

	protected OpCode rdp_set_combine = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			combiners.combine.setMuxs0(w1 & 0xFFFFFF);
			combiners.combine.setMuxs1(w2);
			changed |= CHANGED_COMBINE;
		}
	};

	protected OpCode rdp_set_texture_image = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cache.setTextureImage(w1, w2);
		}
	};

	protected OpCode rdp_set_mask_image = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			int address = w2 & 0x3FFFFFF;
			DepthBufferStack.DepthBuffer buffer = depthBuffers.top;
			// Search through saved depth buffers
			while (buffer != null)
			{
				if (buffer.address == address)
				{
					depthBuffers.moveToTop(buffer);
					current = buffer;
					return;
				}
				buffer = buffer.lower;
			}
			buffer = new DepthBufferStack.DepthBuffer();
			depthBuffers.addTop(buffer);
			buffer.address = address;
			buffer.cleared = true;
			current = buffer;
			if (current.cleared)
				glClearDepthBuffer(RDP_GETOM_Z_UPDATE_EN(otherMode) != 0);
			depthImageAddress = address;
		}
	};

	protected OpCode rdp_set_color_image = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			int address = w2 & 0x3FFFFFF;
			int width = (w1 & 0xFFF) + 1;
			if (colorImage.address != address)
			{
				// colorImage.changed = false;

				if (width == screenWidth)
					colorImage.height = screenHeight;
				else
					colorImage.height = 1;
			}

			// colorImage.format = (w1 >> 21) & 0x7;
			// colorImage.size = (w1 >> 19) & 0x3;
			// colorImage.width = width;
			colorImage.address = address;
		}
	};

	protected OpCode rdp_invalid = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			System.out.printf("RDP: invalid command  %d, %s %s\n", (w1 >> 24) & 0x3f, Integer.toHexString(w1), Integer.toHexString(w2));
		}
	};

	protected OpCode rdp_hle_triangle = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			Vertex[] tmpvertices = { new Vertex(), new Vertex(), new Vertex() };
			tmpvertices[0].vtx = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[8] & 0xFF) << 24) | ((rdp_cmd_data[9] & 0xFF) << 16) | ((rdp_cmd_data[10] & 0xFF) << 8) | (rdp_cmd_data[11] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[12] & 0xFF) << 24) | ((rdp_cmd_data[13] & 0xFF) << 16) | ((rdp_cmd_data[14] & 0xFF) << 8) | (rdp_cmd_data[15] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[16] & 0xFF) << 24) | ((rdp_cmd_data[17] & 0xFF) << 16) | ((rdp_cmd_data[18] & 0xFF) << 8) | (rdp_cmd_data[19] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[20] & 0xFF) << 24) | ((rdp_cmd_data[21] & 0xFF) << 16) | ((rdp_cmd_data[22] & 0xFF) << 8) | (rdp_cmd_data[23] & 0xFF)) };
			tmpvertices[1].vtx = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[24] & 0xFF) << 24) | ((rdp_cmd_data[25] & 0xFF) << 16) | ((rdp_cmd_data[26] & 0xFF) << 8) | (rdp_cmd_data[27] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[28] & 0xFF) << 24) | ((rdp_cmd_data[29] & 0xFF) << 16) | ((rdp_cmd_data[30] & 0xFF) << 8) | (rdp_cmd_data[31] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[32] & 0xFF) << 24) | ((rdp_cmd_data[33] & 0xFF) << 16) | ((rdp_cmd_data[34] & 0xFF) << 8) | (rdp_cmd_data[35] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[36] & 0xFF) << 24) | ((rdp_cmd_data[37] & 0xFF) << 16) | ((rdp_cmd_data[38] & 0xFF) << 8) | (rdp_cmd_data[39] & 0xFF)) };
			tmpvertices[2].vtx = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[40] & 0xFF) << 24) | ((rdp_cmd_data[41] & 0xFF) << 16) | ((rdp_cmd_data[42] & 0xFF) << 8) | (rdp_cmd_data[43] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[44] & 0xFF) << 24) | ((rdp_cmd_data[45] & 0xFF) << 16) | ((rdp_cmd_data[46] & 0xFF) << 8) | (rdp_cmd_data[47] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[48] & 0xFF) << 24) | ((rdp_cmd_data[49] & 0xFF) << 16) | ((rdp_cmd_data[50] & 0xFF) << 8) | (rdp_cmd_data[51] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[52] & 0xFF) << 24) | ((rdp_cmd_data[53] & 0xFF) << 16) | ((rdp_cmd_data[54] & 0xFF) << 8) | (rdp_cmd_data[55] & 0xFF)) };
			tmpvertices[0].color = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[56] & 0xFF) << 24) | ((rdp_cmd_data[57] & 0xFF) << 16) | ((rdp_cmd_data[58] & 0xFF) << 8) | (rdp_cmd_data[59] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[60] & 0xFF) << 24) | ((rdp_cmd_data[61] & 0xFF) << 16) | ((rdp_cmd_data[62] & 0xFF) << 8) | (rdp_cmd_data[63] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[64] & 0xFF) << 24) | ((rdp_cmd_data[65] & 0xFF) << 16) | ((rdp_cmd_data[66] & 0xFF) << 8) | (rdp_cmd_data[67] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[68] & 0xFF) << 24) | ((rdp_cmd_data[69] & 0xFF) << 16) | ((rdp_cmd_data[70] & 0xFF) << 8) | (rdp_cmd_data[71] & 0xFF)) };
			tmpvertices[1].color = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[72] & 0xFF) << 24) | ((rdp_cmd_data[73] & 0xFF) << 16) | ((rdp_cmd_data[74] & 0xFF) << 8) | (rdp_cmd_data[75] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[76] & 0xFF) << 24) | ((rdp_cmd_data[77] & 0xFF) << 16) | ((rdp_cmd_data[78] & 0xFF) << 8) | (rdp_cmd_data[79] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[80] & 0xFF) << 24) | ((rdp_cmd_data[81] & 0xFF) << 16) | ((rdp_cmd_data[82] & 0xFF) << 8) | (rdp_cmd_data[83] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[84] & 0xFF) << 24) | ((rdp_cmd_data[85] & 0xFF) << 16) | ((rdp_cmd_data[86] & 0xFF) << 8) | (rdp_cmd_data[87] & 0xFF)) };
			tmpvertices[2].color = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[88] & 0xFF) << 24) | ((rdp_cmd_data[89] & 0xFF) << 16) | ((rdp_cmd_data[90] & 0xFF) << 8) | (rdp_cmd_data[91] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[92] & 0xFF) << 24) | ((rdp_cmd_data[93] & 0xFF) << 16) | ((rdp_cmd_data[94] & 0xFF) << 8) | (rdp_cmd_data[95] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[96] & 0xFF) << 24) | ((rdp_cmd_data[97] & 0xFF) << 16) | ((rdp_cmd_data[98] & 0xFF) << 8) | (rdp_cmd_data[99] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[100] & 0xFF) << 24) | ((rdp_cmd_data[101] & 0xFF) << 16) | ((rdp_cmd_data[102] & 0xFF) << 8) | (rdp_cmd_data[103] & 0xFF)) };
			tmpvertices[0].tex = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[104] & 0xFF) << 24) | ((rdp_cmd_data[105] & 0xFF) << 16) | ((rdp_cmd_data[106] & 0xFF) << 8) | (rdp_cmd_data[107] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[108] & 0xFF) << 24) | ((rdp_cmd_data[109] & 0xFF) << 16) | ((rdp_cmd_data[110] & 0xFF) << 8) | (rdp_cmd_data[111] & 0xFF)) };
			tmpvertices[1].tex = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[112] & 0xFF) << 24) | ((rdp_cmd_data[113] & 0xFF) << 16) | ((rdp_cmd_data[114] & 0xFF) << 8) | (rdp_cmd_data[115] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[116] & 0xFF) << 24) | ((rdp_cmd_data[117] & 0xFF) << 16) | ((rdp_cmd_data[118] & 0xFF) << 8) | (rdp_cmd_data[119] & 0xFF)) };
			tmpvertices[2].tex = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[120] & 0xFF) << 24) | ((rdp_cmd_data[121] & 0xFF) << 16) | ((rdp_cmd_data[122] & 0xFF) << 8) | (rdp_cmd_data[123] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[124] & 0xFF) << 24) | ((rdp_cmd_data[125] & 0xFF) << 16) | ((rdp_cmd_data[126] & 0xFF) << 8) | (rdp_cmd_data[127] & 0xFF)) };
			tmpvertices[0].clip = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[128] & 0xFF) << 24) | ((rdp_cmd_data[129] & 0xFF) << 16) | ((rdp_cmd_data[130] & 0xFF) << 8) | (rdp_cmd_data[131] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[132] & 0xFF) << 24) | ((rdp_cmd_data[133] & 0xFF) << 16) | ((rdp_cmd_data[134] & 0xFF) << 8) | (rdp_cmd_data[135] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[136] & 0xFF) << 24) | ((rdp_cmd_data[137] & 0xFF) << 16) | ((rdp_cmd_data[138] & 0xFF) << 8) | (rdp_cmd_data[139] & 0xFF)) };
			tmpvertices[1].clip = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[144] & 0xFF) << 24) | ((rdp_cmd_data[145] & 0xFF) << 16) | ((rdp_cmd_data[146] & 0xFF) << 8) | (rdp_cmd_data[147] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[148] & 0xFF) << 24) | ((rdp_cmd_data[149] & 0xFF) << 16) | ((rdp_cmd_data[150] & 0xFF) << 8) | (rdp_cmd_data[151] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[152] & 0xFF) << 24) | ((rdp_cmd_data[153] & 0xFF) << 16) | ((rdp_cmd_data[154] & 0xFF) << 8) | (rdp_cmd_data[155] & 0xFF)) };
			tmpvertices[2].clip = new float[] {
					Float.intBitsToFloat(((rdp_cmd_data[160] & 0xFF) << 24) | ((rdp_cmd_data[161] & 0xFF) << 16) | ((rdp_cmd_data[162] & 0xFF) << 8) | (rdp_cmd_data[163] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[164] & 0xFF) << 24) | ((rdp_cmd_data[165] & 0xFF) << 16) | ((rdp_cmd_data[166] & 0xFF) << 8) | (rdp_cmd_data[167] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[168] & 0xFF) << 24) | ((rdp_cmd_data[169] & 0xFF) << 16) | ((rdp_cmd_data[170] & 0xFF) << 8) | (rdp_cmd_data[171] & 0xFF)) };

			// NoN work-around, clips triangles, and draws the clipped-off parts with clamped z
			if ((rdp_cmd_data[1] & 0xFF) != 0 &&
					((tmpvertices[0].clip[2] < 0.0f) ||
							(tmpvertices[1].clip[2] < 0.0f) ||
					(tmpvertices[2].clip[2] < 0.0f)))
			{
				Vertex[] nearVertices = new Vertex[4];
				Vertex[] clippedVertices = new Vertex[4];
				for (int i = 0; i < 4; i++)
				{ // TMP
					nearVertices[i] = new Vertex();
					clippedVertices[i] = new Vertex();
				}

				int nearIndex = 0;
				int clippedIndex = 0;

				int[] v = { 0, 1, 2 };

				for (int i = 0; i < 3; i++)
				{
					int j = i + 1;
					if (j == 3) j = 0;

					if (((tmpvertices[v[i]].clip[2] < 0.0f) && (tmpvertices[v[j]].clip[2] >= 0.0f)) ||
							((tmpvertices[v[i]].clip[2] >= 0.0f) && (tmpvertices[v[j]].clip[2] < 0.0f)))
					{
						float percent = (-tmpvertices[v[i]].vtx[3] - tmpvertices[v[i]].vtx[2]) / ((tmpvertices[v[j]].vtx[2] - tmpvertices[v[i]].vtx[2]) + (tmpvertices[v[j]].vtx[3] - tmpvertices[v[i]].vtx[3]));

						clippedVertices[clippedIndex].interpolateVertex(percent, tmpvertices[v[i]], tmpvertices[v[j]]);

						nearVertices[nearIndex].copyVertex(clippedVertices[clippedIndex]);
						nearVertices[nearIndex].vtx[2] = -nearVertices[nearIndex].vtx[3];

						clippedIndex++;
						nearIndex++;
					}

					if (((tmpvertices[v[i]].clip[2] < 0.0f) && (tmpvertices[v[j]].clip[2] >= 0.0f)) ||
							((tmpvertices[v[i]].clip[2] >= 0.0f) && (tmpvertices[v[j]].clip[2] >= 0.0f)))
					{
						clippedVertices[clippedIndex].copyVertex(tmpvertices[v[j]]);
						clippedIndex++;
					}
					else
					{
						nearVertices[nearIndex].copyVertex(tmpvertices[v[j]]);
						nearVertices[nearIndex].vtx[2] = -nearVertices[nearIndex].vtx[3];
						nearIndex++;
					}
				}

				addTriangle(clippedVertices[0].vtx, clippedVertices[0].color, clippedVertices[0].tex,
						clippedVertices[1].vtx, clippedVertices[1].color, clippedVertices[1].tex,
						clippedVertices[2].vtx, clippedVertices[2].color, clippedVertices[2].tex);

				if (clippedIndex == 4)
				{
					addTriangle(clippedVertices[0].vtx, clippedVertices[0].color, clippedVertices[0].tex,
							clippedVertices[2].vtx, clippedVertices[2].color, clippedVertices[2].tex,
							clippedVertices[3].vtx, clippedVertices[3].color, clippedVertices[3].tex);
				}

				gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);

				addTriangle(nearVertices[0].vtx, nearVertices[0].color, nearVertices[0].tex,
						nearVertices[1].vtx, nearVertices[1].color, nearVertices[1].tex,
						nearVertices[2].vtx, nearVertices[2].color, nearVertices[2].tex);

				if (nearIndex == 4)
				{
					addTriangle(nearVertices[0].vtx, nearVertices[0].color, nearVertices[0].tex,
							nearVertices[2].vtx, nearVertices[2].color, nearVertices[2].tex,
							nearVertices[3].vtx, nearVertices[3].color, nearVertices[3].tex);
				}

				if (((otherMode.w1 >> 10) & 0x3) == ZMODE_DEC)
					gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);

			}
			else
			{
				addTriangle(tmpvertices[0].vtx, tmpvertices[0].color, tmpvertices[0].tex,
						tmpvertices[1].vtx, tmpvertices[1].color, tmpvertices[1].tex,
						tmpvertices[2].vtx, tmpvertices[2].color, tmpvertices[2].tex);
			}

			update();
		}
	};

	protected OpCode rdp_hle_line = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			float[] vtx1 = {
					Float.intBitsToFloat(w2),
					Float.intBitsToFloat(((rdp_cmd_data[8] & 0xFF) << 24) | ((rdp_cmd_data[9] & 0xFF) << 16) | ((rdp_cmd_data[10] & 0xFF) << 8) | (rdp_cmd_data[11] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[12] & 0xFF) << 24) | ((rdp_cmd_data[13] & 0xFF) << 16) | ((rdp_cmd_data[14] & 0xFF) << 8) | (rdp_cmd_data[15] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[16] & 0xFF) << 24) | ((rdp_cmd_data[17] & 0xFF) << 16) | ((rdp_cmd_data[18] & 0xFF) << 8) | (rdp_cmd_data[19] & 0xFF)) };
			float[] c1 = {
					Float.intBitsToFloat(((rdp_cmd_data[20] & 0xFF) << 24) | ((rdp_cmd_data[21] & 0xFF) << 16) | ((rdp_cmd_data[22] & 0xFF) << 8) | (rdp_cmd_data[23] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[24] & 0xFF) << 24) | ((rdp_cmd_data[25] & 0xFF) << 16) | ((rdp_cmd_data[26] & 0xFF) << 8) | (rdp_cmd_data[27] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[28] & 0xFF) << 24) | ((rdp_cmd_data[29] & 0xFF) << 16) | ((rdp_cmd_data[30] & 0xFF) << 8) | (rdp_cmd_data[31] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[32] & 0xFF) << 24) | ((rdp_cmd_data[33] & 0xFF) << 16) | ((rdp_cmd_data[34] & 0xFF) << 8) | (rdp_cmd_data[35] & 0xFF)) };

			float[] vtx2 = {
					Float.intBitsToFloat(((rdp_cmd_data[36] & 0xFF) << 24) | ((rdp_cmd_data[37] & 0xFF) << 16) | ((rdp_cmd_data[38] & 0xFF) << 8) | (rdp_cmd_data[39] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[40] & 0xFF) << 24) | ((rdp_cmd_data[41] & 0xFF) << 16) | ((rdp_cmd_data[42] & 0xFF) << 8) | (rdp_cmd_data[43] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[44] & 0xFF) << 24) | ((rdp_cmd_data[45] & 0xFF) << 16) | ((rdp_cmd_data[46] & 0xFF) << 8) | (rdp_cmd_data[47] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[48] & 0xFF) << 24) | ((rdp_cmd_data[49] & 0xFF) << 16) | ((rdp_cmd_data[50] & 0xFF) << 8) | (rdp_cmd_data[51] & 0xFF)) };
			float[] c2 = {
					Float.intBitsToFloat(((rdp_cmd_data[52] & 0xFF) << 24) | ((rdp_cmd_data[53] & 0xFF) << 16) | ((rdp_cmd_data[54] & 0xFF) << 8) | (rdp_cmd_data[55] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[56] & 0xFF) << 24) | ((rdp_cmd_data[57] & 0xFF) << 16) | ((rdp_cmd_data[58] & 0xFF) << 8) | (rdp_cmd_data[59] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[60] & 0xFF) << 24) | ((rdp_cmd_data[61] & 0xFF) << 16) | ((rdp_cmd_data[62] & 0xFF) << 8) | (rdp_cmd_data[63] & 0xFF)),
					Float.intBitsToFloat(((rdp_cmd_data[64] & 0xFF) << 24) | ((rdp_cmd_data[65] & 0xFF) << 16) | ((rdp_cmd_data[66] & 0xFF) << 8) | (rdp_cmd_data[67] & 0xFF)) };

			glDrawLine(vtx1, c1, vtx2, c2,
					Float.intBitsToFloat(((rdp_cmd_data[68] & 0xFF) << 24) | ((rdp_cmd_data[69] & 0xFF) << 16) | ((rdp_cmd_data[70] & 0xFF) << 8) | (rdp_cmd_data[71] & 0xFF)));
		}
	};

	protected OpCode rdp_hle_texture = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cache.setTexture(w1, w2);
			changed |= CHANGED_TEXTURE;
		}
	};

	// LLE DP TEST /////////////////////////////////////////////////////////////

	private static float zscale(int z)
	{
		return ((float) (z)) / 0xffff;
	}

	private int RDP_GETOM_PERSP_TEX_EN(OtherMode om)
	{
		return ((om).w1 & 0x80000) != 0 ? 1 : 0;
	}

	private float XSCALE(int x)
	{
		return ((float) x) / (1 << 18);
	}

	private float YSCALE(int y)
	{
		return ((float) y) / (1 << 2);
	}

	private float ZSCALE(int z)
	{
		return RDP_GETOM_Z_SOURCE_SEL(otherMode) != 0 ? zDepth : zscale(z >> 16);
	}

	private float WSCALE(int z)
	{
		return 1.0f / (RDP_GETOM_PERSP_TEX_EN(otherMode) != 0 ? (((float) (z + 0x10000)) / 0xffff0000) : 1.0f);
	}

	private int CSCALE(int c)
	{
		return ((c) > 0x3ff0000 ? 0x3ff0000 : ((c) < 0 ? 0 : (c))) >> 18;
	}

	private long PERSP(int s, int w)
	{
		return ((long) (s) << 20) / (w != 0 ? w : 1);
	}

	private float SSCALE(int s, int _w)
	{
		return RDP_GETOM_PERSP_TEX_EN(otherMode) != 0 ? ((float) (PERSP(s, _w))) / (1 << 10) : ((float) (s)) / (1 << 21);
	}

	private float TSCALE(int s, int w)
	{
		return RDP_GETOM_PERSP_TEX_EN(otherMode) != 0 ? ((float) (PERSP(s, w))) / (1 << 10) : ((float) (s)) / (1 << 21);
	}

	private void rglTriangle(int w1, int w2, int shade, int texture, int zbuffer, byte[] rdp_cmd, int rdp_cmd_offset)
	{
		Vertex[] tmpvertices = { new Vertex(), new Vertex(), new Vertex(), new Vertex(), new Vertex(), new Vertex() };
		int tilenum = (w1 >> 16) & 0x7;
		int j;
		int xleft, xright, xleft_inc, xright_inc;
		int xstart, xend;
		int r, g, b, a, z, s, t, w;
		int dr, dg, db, da;
		int drdx = 0, dgdx = 0, dbdx = 0, dadx = 0, dzdx = 0, dsdx = 0, dtdx = 0, dwdx = 0;
		int drdy = 0, dgdy = 0, dbdy = 0, dady = 0, dzdy = 0, dsdy = 0, dtdy = 0, dwdy = 0;
		int drde = 0, dgde = 0, dbde = 0, dade = 0, dzde = 0, dsde = 0, dtde = 0, dwde = 0;
		int flip = (w1 & 0x800000) != 0 ? 1 : 0;

		int yl, ym, yh;
		int xl, xm, xh;
		long dxldy, dxhdy, dxmdy;
		int w3, w4, w5, w6, w7, w8;

		int shade_base = rdp_cmd_offset + 8 * 4;
		int texture_base = rdp_cmd_offset + 8 * 4;
		int zbuffer_base = rdp_cmd_offset + 8 * 4;

		// int t1 = (texture && rglT1Usage(rdpState))? RGL_STRIP_TEX1:0;
		// int t2 = (texture && tilenum < 7 && rglT2Usage(rdpState))? RGL_STRIP_TEX2:0;
		// if (t1)
		// rglPrepareRendering(1, (tilenum==7 && RDP_GETOM_CYCLE_TYPE(rdpState.otherModes)==1)? 0:tilenum, 0, zbuffer);
		// if (t2)
		// rglPrepareRendering(1, tilenum+1, 0, zbuffer);
		// else if (!t1)
		// Rgl.rglPrepareRendering(0, 0, 0, zbuffer);

		// curRBuffer->flags |= RGL_RB_HASTRIANGLES;

		if (shade != 0)
		{
			texture_base += 16 * 4;
			zbuffer_base += 16 * 4;
		}
		if (texture != 0)
		{
			zbuffer_base += 16 * 4;
		}

		int index;
		index = rdp_cmd_offset + 2 * 4;
		w3 = ((rdp_cmd[index] & 0xFF) << 24) | ((rdp_cmd[index + 1] & 0xFF) << 16) | ((rdp_cmd[index + 2] & 0xFF) << 8) | (rdp_cmd[index + 3] & 0xFF);
		index = rdp_cmd_offset + 3 * 4;
		w4 = ((rdp_cmd[index] & 0xFF) << 24) | ((rdp_cmd[index + 1] & 0xFF) << 16) | ((rdp_cmd[index + 2] & 0xFF) << 8) | (rdp_cmd[index + 3] & 0xFF);
		index = rdp_cmd_offset + 4 * 4;
		w5 = ((rdp_cmd[index] & 0xFF) << 24) | ((rdp_cmd[index + 1] & 0xFF) << 16) | ((rdp_cmd[index + 2] & 0xFF) << 8) | (rdp_cmd[index + 3] & 0xFF);
		index = rdp_cmd_offset + 5 * 4;
		w6 = ((rdp_cmd[index] & 0xFF) << 24) | ((rdp_cmd[index + 1] & 0xFF) << 16) | ((rdp_cmd[index + 2] & 0xFF) << 8) | (rdp_cmd[index + 3] & 0xFF);
		index = rdp_cmd_offset + 6 * 4;
		w7 = ((rdp_cmd[index] & 0xFF) << 24) | ((rdp_cmd[index + 1] & 0xFF) << 16) | ((rdp_cmd[index + 2] & 0xFF) << 8) | (rdp_cmd[index + 3] & 0xFF);
		index = rdp_cmd_offset + 7 * 4;
		w8 = ((rdp_cmd[index] & 0xFF) << 24) | ((rdp_cmd[index + 1] & 0xFF) << 16) | ((rdp_cmd[index + 2] & 0xFF) << 8) | (rdp_cmd[index + 3] & 0xFF);

		yl = (w1 & 0x3fff);
		yl = ((int) ((short) (yl << 2))) >> 2;
		ym = ((w2 >> 16) & 0x3fff);
		ym = ((int) ((short) (ym << 2))) >> 2;
		yh = ((w2 >> 0) & 0x3fff);
		yh = ((int) ((short) (yh << 2))) >> 2;
		xl = (int) (w3);
		xh = (int) (w5);
		xm = (int) (w7);
		dxldy = (int) (w4);
		dxhdy = (int) (w6);
		dxmdy = (int) (w8);

		// System.out.printf("LLE yl=%d, ym=%d, yh=%d\n",
		// yl, ym, yh);

		if ((yl & (0x800 << 2)) != 0)
		{
			yl |= 0xfffff000 << 2;
		}
		if ((ym & (0x800 << 2)) != 0)
		{
			ym |= 0xfffff000 << 2;
		}
		if ((yh & (0x800 << 2)) != 0)
		{
			yh |= 0xfffff000 << 2;
		}

		yh &= ~3;

		System.out.printf("LLE yl=%d, ym=%d, yh=%d\n",
				yl, ym, yh);

		r = 0xff;
		g = 0xff;
		b = 0xff;
		a = 0xff;
		z = 0xffff0000;
		s = 0;
		t = 0;
		w = 0x30000;
		dr = 0;
		dg = 0;
		db = 0;
		da = 0;

		if (shade != 0)
		{
			r = ((rdp_cmd[shade_base + 0 * 4 + 0] & 0xFF) << 24) | ((rdp_cmd[shade_base + 1] & 0xFF) << 16) | ((rdp_cmd[shade_base + 4 * 4 + 0] & 0xFF) << 8) | (rdp_cmd[shade_base + 4 * 4 + 1] & 0xFF);
			g = ((rdp_cmd[shade_base + 0 * 4 + 2] & 0xFF) << 24) | ((rdp_cmd[shade_base + 0 * 4 + 3] & 0xFF) << 16) | ((rdp_cmd[shade_base + 4 * 4 + 2] & 0xFF) << 8) | (rdp_cmd[shade_base + 4 * 4 + 3] & 0xFF);
			b = ((rdp_cmd[shade_base + 0 * 4 + 4] & 0xFF) << 24) | ((rdp_cmd[shade_base + 0 * 4 + 5] & 0xFF) << 16) | ((rdp_cmd[shade_base + 4 * 4 + 4] & 0xFF) << 8) | (rdp_cmd[shade_base + 4 * 4 + 5] & 0xFF);
			a = ((rdp_cmd[shade_base + 0 * 4 + 6] & 0xFF) << 24) | ((rdp_cmd[shade_base + 0 * 4 + 7] & 0xFF) << 16) | ((rdp_cmd[shade_base + 4 * 4 + 6] & 0xFF) << 8) | (rdp_cmd[shade_base + 4 * 4 + 7] & 0xFF);
			drdx = ((rdp_cmd[shade_base + 2 * 4 + 0] & 0xFF) << 24) | ((rdp_cmd[shade_base + 2 * 4 + 1] & 0xFF) << 16) | ((rdp_cmd[shade_base + 6 * 4 + 0] & 0xFF) << 8) | (rdp_cmd[shade_base + 6 * 4 + 1] & 0xFF);
			dgdx = ((rdp_cmd[shade_base + 2 * 4 + 2] & 0xFF) << 24) | ((rdp_cmd[shade_base + 2 * 4 + 3] & 0xFF) << 16) | ((rdp_cmd[shade_base + 6 * 4 + 2] & 0xFF) << 8) | (rdp_cmd[shade_base + 6 * 4 + 3] & 0xFF);
			dbdx = ((rdp_cmd[shade_base + 2 * 4 + 4] & 0xFF) << 24) | ((rdp_cmd[shade_base + 2 * 4 + 5] & 0xFF) << 16) | ((rdp_cmd[shade_base + 6 * 4 + 4] & 0xFF) << 8) | (rdp_cmd[shade_base + 6 * 4 + 5] & 0xFF);
			dadx = ((rdp_cmd[shade_base + 2 * 4 + 6] & 0xFF) << 24) | ((rdp_cmd[shade_base + 2 * 4 + 7] & 0xFF) << 16) | ((rdp_cmd[shade_base + 6 * 4 + 6] & 0xFF) << 8) | (rdp_cmd[shade_base + 6 * 4 + 7] & 0xFF);
			drde = ((rdp_cmd[shade_base + 8 * 4 + 0] & 0xFF) << 24) | ((rdp_cmd[shade_base + 8 * 4 + 1] & 0xFF) << 16) | ((rdp_cmd[shade_base + 12 * 4 + 0] & 0xFF) << 8) | (rdp_cmd[shade_base + 12 * 4 + 1] & 0xFF);
			dgde = ((rdp_cmd[shade_base + 8 * 4 + 2] & 0xFF) << 24) | ((rdp_cmd[shade_base + 8 * 4 + 3] & 0xFF) << 16) | ((rdp_cmd[shade_base + 12 * 4 + 2] & 0xFF) << 8) | (rdp_cmd[shade_base + 12 * 4 + 3] & 0xFF);
			dbde = ((rdp_cmd[shade_base + 8 * 4 + 4] & 0xFF) << 24) | ((rdp_cmd[shade_base + 8 * 4 + 5] & 0xFF) << 16) | ((rdp_cmd[shade_base + 12 * 4 + 4] & 0xFF) << 8) | (rdp_cmd[shade_base + 12 * 4 + 5] & 0xFF);
			dade = ((rdp_cmd[shade_base + 8 * 4 + 6] & 0xFF) << 24) | ((rdp_cmd[shade_base + 8 * 4 + 7] & 0xFF) << 16) | ((rdp_cmd[shade_base + 12 * 4 + 6] & 0xFF) << 8) | (rdp_cmd[shade_base + 12 * 4 + 7] & 0xFF);
			drdy = ((rdp_cmd[shade_base + 10 * 4 + 0] & 0xFF) << 24) | ((rdp_cmd[shade_base + 10 * 4 + 1] & 0xFF) << 16) | ((rdp_cmd[shade_base + 14 * 4 + 0] & 0xFF) << 8) | (rdp_cmd[shade_base + 14 * 4 + 1] & 0xFF);
			dgdy = ((rdp_cmd[shade_base + 10 * 4 + 2] & 0xFF) << 24) | ((rdp_cmd[shade_base + 10 * 4 + 3] & 0xFF) << 16) | ((rdp_cmd[shade_base + 14 * 4 + 2] & 0xFF) << 8) | (rdp_cmd[shade_base + 14 * 4 + 3] & 0xFF);
			dbdy = ((rdp_cmd[shade_base + 10 * 4 + 4] & 0xFF) << 24) | ((rdp_cmd[shade_base + 10 * 4 + 5] & 0xFF) << 16) | ((rdp_cmd[shade_base + 14 * 4 + 4] & 0xFF) << 8) | (rdp_cmd[shade_base + 14 * 4 + 5] & 0xFF);
			dady = ((rdp_cmd[shade_base + 10 * 4 + 6] & 0xFF) << 24) | ((rdp_cmd[shade_base + 10 * 4 + 7] & 0xFF) << 16) | ((rdp_cmd[shade_base + 14 * 4 + 6] & 0xFF) << 8) | (rdp_cmd[shade_base + 14 * 4 + 7] & 0xFF);
		}
		if (texture != 0)
		{
			s = (rdp_cmd[texture_base + 0 * 4] & 0xffff0000) | ((rdp_cmd[texture_base + 4 * 4] >> 16) & 0x0000ffff);
			t = ((rdp_cmd[texture_base + 0 * 4] << 16) & 0xffff0000) | (rdp_cmd[texture_base + 4 * 4] & 0x0000ffff);
			w = (rdp_cmd[texture_base + 1 * 4] & 0xffff0000) | ((rdp_cmd[texture_base + 5 * 4] >> 16) & 0x0000ffff);
			dsdx = (rdp_cmd[texture_base + 2 * 4] & 0xffff0000) | ((rdp_cmd[texture_base + 6 * 4] >> 16) & 0x0000ffff);
			dtdx = ((rdp_cmd[texture_base + 2 * 4] << 16) & 0xffff0000) | (rdp_cmd[texture_base + 6 * 4] & 0x0000ffff);
			dwdx = (rdp_cmd[texture_base + 3 * 4] & 0xffff0000) | ((rdp_cmd[texture_base + 7 * 4] >> 16) & 0x0000ffff);
			dsde = (rdp_cmd[texture_base + 8 * 4] & 0xffff0000) | ((rdp_cmd[texture_base + 12 * 4] >> 16) & 0x0000ffff);
			dtde = ((rdp_cmd[texture_base + 8 * 4] << 16) & 0xffff0000) | (rdp_cmd[texture_base + 12 * 4] & 0x0000ffff);
			dwde = (rdp_cmd[texture_base + 9 * 4] & 0xffff0000) | ((rdp_cmd[texture_base + 13 * 4] >> 16) & 0x0000ffff);
			dsdy = (rdp_cmd[texture_base + 10 * 4] & 0xffff0000) | ((rdp_cmd[texture_base + 14 * 4] >> 16) & 0x0000ffff);
			dtdy = ((rdp_cmd[texture_base + 10 * 4] << 16) & 0xffff0000) | (rdp_cmd[texture_base + 14 * 4] & 0x0000ffff);
			dwdy = (rdp_cmd[texture_base + 11 * 4] & 0xffff0000) | ((rdp_cmd[texture_base + 15 * 4] >> 16) & 0x0000ffff);
		}
		if (zbuffer != 0)
		{
			z = rdp_cmd[zbuffer_base + 0 * 4];
			dzdx = rdp_cmd[zbuffer_base + 1 * 4];
			dzde = rdp_cmd[zbuffer_base + 2 * 4];
			dzdy = rdp_cmd[zbuffer_base + 3 * 4];
		}

		xh <<= 2;
		xm <<= 2;
		xl <<= 2;
		r <<= 2;
		g <<= 2;
		b <<= 2;
		a <<= 2;
		dsde >>= 2;
		dtde >>= 2;
		dsdx >>= 2;
		dtdx >>= 2;
		dzdx >>= 2;
		dzde >>= 2;
		dzdy >>= 2;
		dwdx >>= 2;
		dwde >>= 2;
		dwdy >>= 2;

		xleft = xm;
		xright = xh;
		xleft_inc = (int) dxmdy;
		xright_inc = (int) dxhdy;

		System.out.printf("LLE xl=%d, dxldy=%d," +
				"xh=%d, dxhdy=%d, xm=%d, dxmdy=%d\n",
				xl, dxldy, xh, dxhdy, xm, dxmdy);

		System.out.printf("LLE r=%d, g=%d, b=%d, a=%d," +
				" drde=%d, dgde=%d, dbde=%d, dade=%d," +
				" drdx=%d, dgdx=%d, dbdx=%d, dadx=%d\n",
				r, g, b, a, drde, dgde, dbde, dade, drdx, dgdx, dbdx, dadx);

		while (yh < ym &&
				!((flip == 0 && xleft < xright + 0x10000) ||
				(flip != 0 && xleft > xright - 0x10000)))
		{
			xleft += xleft_inc;
			xright += xright_inc;
			s += dsde;
			t += dtde;
			w += dwde;
			r += drde;
			g += dgde;
			b += dbde;
			a += dade;
			z += dzde;
			yh++;
		}

		j = ym - yh;

		// rglStrip_t * strip = strips + nbStrips++;
		// rglAssert(nbStrips < MAX_STRIPS);
		// curChunk->nbStrips++;
		// rglVertex_t * vtx = vtxs + nbVtxs;
		//
		// strip->flags = (shade? RGL_STRIP_SHADE : 0) | t1 | t2
		// | RGL_STRIP_ZBUFFER;
		// strip->vtxs = vtx;
		// strip->tilenum = tilenum;

		int vtx = 0;

		int sw;
		if (j > 0)
		{
			int dx = (xleft - xright >> 16);
			System.out.printf("flip=%d, xleft=%d, xright=%d\n", flip, xleft, xright);
			if ((flip == 0 && xleft < xright) ||
					(flip != 0/* && xleft > xright */))
			{
				if (shade != 0)
				{
					System.out.printf("r01=%d\n", r);
					tmpvertices[vtx].color[0] = CSCALE(r + drdx * dx) * 0.0039215689f;
					tmpvertices[vtx].color[1] = CSCALE(g + dgdx * dx) * 0.0039215689f;
					tmpvertices[vtx].color[2] = CSCALE(b + dbdx * dx) * 0.0039215689f;
					tmpvertices[vtx].color[3] = CSCALE(a + dadx * dx) * 0.0039215689f;
				}
				if (texture != 0)
				{
					tmpvertices[vtx].tex[0] = SSCALE(s + dsdx * dx, w + dwdx * dx);
					tmpvertices[vtx].tex[1] = TSCALE(t + dtdx * dx, w + dwdx * dx);
				}
				System.out.printf("x01=%d\n", xleft);
				tmpvertices[vtx].vtx[0] = XSCALE(xleft);
				tmpvertices[vtx].vtx[1] = -YSCALE(yh);
				tmpvertices[vtx].vtx[2] = ZSCALE(z + dzdx * dx);
				tmpvertices[vtx].vtx[3] = WSCALE(w + dwdx * dx);
				vtx++;
			}
			if ((flip == 0/* && xleft < xright */) ||
					(flip != 0 && xleft > xright))
			{
				if (shade != 0)
				{
					System.out.printf("r02=%d\n", r);
					tmpvertices[vtx].color[0] = CSCALE(r) * 0.0039215689f;
					tmpvertices[vtx].color[1] = CSCALE(g) * 0.0039215689f;
					tmpvertices[vtx].color[2] = CSCALE(b) * 0.0039215689f;
					tmpvertices[vtx].color[3] = CSCALE(a) * 0.0039215689f;
				}
				if (texture != 0)
				{
					tmpvertices[vtx].tex[0] = SSCALE(s, w);
					tmpvertices[vtx].tex[1] = TSCALE(t, w);
				}
				System.out.printf("x02=%d\n", xright);
				tmpvertices[vtx].vtx[0] = XSCALE(xright);
				tmpvertices[vtx].vtx[1] = -YSCALE(yh);
				tmpvertices[vtx].vtx[2] = ZSCALE(z);
				tmpvertices[vtx].vtx[3] = WSCALE(w);
				vtx++;
			}
		}
		xleft += xleft_inc * j;
		xright += xright_inc * j;
		s += dsde * j;
		t += dtde * j;
		w += dwde * j;
		r += drde * j;
		g += dgde * j;
		b += dbde * j;
		a += dade * j;
		z += dzde * j;
		// render ...

		xleft = xl;

		{
			int dx = (xleft - xright >> 16);
			System.out.printf("flip=%d, xleft=%d, xright=%d\n", flip, xleft, xright);
			if ((flip == 0 && xleft <= xright) ||
					(flip != 0/* && xleft >= xright */))
			{
				if (shade != 0)
				{
					System.out.printf("r11=%d\n", r);
					tmpvertices[vtx].color[0] = CSCALE(r + drdx * dx) * 0.0039215689f;
					tmpvertices[vtx].color[1] = CSCALE(g + dgdx * dx) * 0.0039215689f;
					tmpvertices[vtx].color[2] = CSCALE(b + dbdx * dx) * 0.0039215689f;
					tmpvertices[vtx].color[3] = CSCALE(a + dadx * dx) * 0.0039215689f;
				}
				if (texture != 0)
				{
					tmpvertices[vtx].tex[0] = SSCALE(s + dsdx * dx, w + dwdx * dx);
					tmpvertices[vtx].tex[1] = TSCALE(t + dtdx * dx, w + dwdx * dx);
				}
				System.out.printf("x11=%d\n", xleft);
				tmpvertices[vtx].vtx[0] = XSCALE(xleft);
				tmpvertices[vtx].vtx[1] = -YSCALE(ym);
				tmpvertices[vtx].vtx[2] = ZSCALE(z + dzdx * dx);
				tmpvertices[vtx].vtx[3] = WSCALE(w + dwdx * dx);
				vtx++;
			}
			if ((flip == 0/* && xleft <= xright */) ||
					(flip != 0 && xleft >= xright))
			{
				if (shade != 0)
				{
					System.out.printf("r12=%d\n", r);
					tmpvertices[vtx].color[0] = CSCALE(r) * 0.0039215689f;
					tmpvertices[vtx].color[1] = CSCALE(g) * 0.0039215689f;
					tmpvertices[vtx].color[2] = CSCALE(b) * 0.0039215689f;
					tmpvertices[vtx].color[3] = CSCALE(a) * 0.0039215689f;
				}
				if (texture != 0)
				{
					tmpvertices[vtx].tex[0] = SSCALE(s, w);
					tmpvertices[vtx].tex[1] = TSCALE(t, w);
				}
				System.out.printf("x12=%d\n", xright);
				tmpvertices[vtx].vtx[0] = XSCALE(xright);
				tmpvertices[vtx].vtx[1] = -YSCALE(ym);
				tmpvertices[vtx].vtx[2] = ZSCALE(z);
				tmpvertices[vtx].vtx[3] = WSCALE(w);
				vtx++;
			}
		}
		xleft_inc = (int) dxldy;
		xright_inc = (int) dxhdy;

		j = yl - ym;
		xleft += xleft_inc * j;
		xright += xright_inc * j;
		s += dsde * j;
		t += dtde * j;
		w += dwde * j;
		r += drde * j;
		g += dgde * j;
		b += dbde * j;
		a += dade * j;
		z += dzde * j;

		while (yl > ym &&
				!((flip == 0 && xleft < xright + 0x10000) ||
				(flip != 0 && xleft > xright - 0x10000)))
		{
			xleft -= xleft_inc;
			xright -= xright_inc;
			s -= dsde;
			t -= dtde;
			w -= dwde;
			r -= drde;
			g -= dgde;
			b -= dbde;
			a -= dade;
			z -= dzde;
			j--;
			yl--;
		}

		// render ...
		if (j >= 0)
		{
			int dx = (xleft - xright >> 16);
			System.out.printf("flip=%d, xleft=%d, xright=%d\n", flip, xleft, xright);
			if ((flip == 0 && xleft <= xright) ||
					(flip != 0/* && xleft >= xright */))
			{
				if (shade != 0)
				{
					System.out.printf("r21=%d\n", r);
					tmpvertices[vtx].color[0] = CSCALE(r + drdx * dx) * 0.0039215689f;
					tmpvertices[vtx].color[1] = CSCALE(g + dgdx * dx) * 0.0039215689f;
					tmpvertices[vtx].color[2] = CSCALE(b + dbdx * dx) * 0.0039215689f;
					tmpvertices[vtx].color[3] = CSCALE(a + dadx * dx) * 0.0039215689f;
				}
				if (texture != 0)
				{
					tmpvertices[vtx].tex[0] = SSCALE(s + dsdx * dx, w + dwdx * dx);
					tmpvertices[vtx].tex[1] = TSCALE(t + dtdx * dx, w + dwdx * dx);
				}
				System.out.printf("x21=%d\n", xleft);
				tmpvertices[vtx].vtx[0] = XSCALE(xleft);
				tmpvertices[vtx].vtx[1] = -YSCALE(yl);
				tmpvertices[vtx].vtx[2] = ZSCALE(z + dzdx * dx);
				tmpvertices[vtx].vtx[3] = WSCALE(w + dwdx * dx);
				vtx++;
			}
			if ((flip == 0/* && xleft <= xright */) ||
					(flip != 0 && xleft >= xright))
			{
				if (shade != 0)
				{
					System.out.printf("r22=%d\n", r);
					tmpvertices[vtx].color[0] = CSCALE(r) * 0.0039215689f;
					tmpvertices[vtx].color[1] = CSCALE(g) * 0.0039215689f;
					tmpvertices[vtx].color[2] = CSCALE(b) * 0.0039215689f;
					tmpvertices[vtx].color[3] = CSCALE(a) * 0.0039215689f;
				}
				if (texture != 0)
				{
					tmpvertices[vtx].tex[0] = SSCALE(s, w);
					tmpvertices[vtx].tex[1] = TSCALE(t, w);
				}
				System.out.printf("x22=%d\n", xright);
				tmpvertices[vtx].vtx[0] = XSCALE(xright);
				tmpvertices[vtx].vtx[1] = -YSCALE(yl);
				tmpvertices[vtx].vtx[2] = ZSCALE(z);
				tmpvertices[vtx].vtx[3] = WSCALE(w);
				vtx++;
			}
		}

		// strip->nbVtxs = vtx - strip->vtxs;
		// nbVtxs = vtx - vtxs;
		addTriangle(tmpvertices[0].vtx, tmpvertices[0].color, tmpvertices[0].tex,
				tmpvertices[2].vtx, tmpvertices[2].color, tmpvertices[2].tex,
				tmpvertices[5].vtx, tmpvertices[5].color, tmpvertices[5].tex);
	}
}
