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

package jario.n64.console.rcp.textures;

import jario.hardware.Bus16bit;
import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Hardware;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import javax.media.opengl.GL2;

public class TextureCache
{
	// GBI
	// Image formats
	public static final int G_IM_FMT_RGBA = 0;
	public static final int G_IM_FMT_YUV = 1;
	public static final int G_IM_FMT_CI = 2;
	public static final int G_IM_FMT_IA = 3;
	public static final int G_IM_FMT_I = 4;
	// Image sizes
	public static final int G_IM_SIZ_4b = 0;
	public static final int G_IM_SIZ_8b = 1;
	public static final int G_IM_SIZ_16b = 2;
	public static final int G_IM_SIZ_32b = 3;
	public static final int G_IM_SIZ_DD = 5;

	public static final int TEXTUREMODE_NORMAL = 0;
	public static final int TEXTUREMODE_TEXRECT = 1;
	public static final int TEXTUREMODE_BGIMAGE = 2;
	// public static final int TEXTUREMODE_FRAMEBUFFER= 3;

	public static final int LOADTYPE_BLOCK = 0;
	public static final int LOADTYPE_TILE = 1;

	public static final int CHANGED_TMEM = 0x008;

	private static final int CRC32_POLYNOMIAL = 0x04C11DB7;

	private static class BgImage
	{
		public int address;
		public int width;
		public int height;
		public int format;
		public int size;
		public int palette;
	};

	private static class TextureImage
	{
		public int format;
		public int size;
		public int width;
		public int bpl;
		public int address;
	};

	private static class TexRect
	{
		public int width;
		public int height;
	};

	private static class Texture
	{
		public float scales;
		public float scalet;
		public int level;
		public int on;
		public int tile;
	};

	// used by Gdp
	public static class gDPTile
	{
		int format, size, line, tmem, palette;

		public int maskt, masks;
		public int shiftt, shifts;
		public float fuls, fult, flrs, flrt;
		public int uls, ult, lrs, lrt;

		public int mirrort; // : 1;
		public int clampt; // : 1;
		public int pad0; // : 30;

		public int mirrors; // : 1;
		public int clamps; // : 1;
		public int pad1; // : 30;

		public void setCmt(int value)
		{
			pad0 = (value >> 2) & 0x3FFFFFFF;
			clampt = (value >> 1) & 1;
			mirrort = (value) & 1;
		}

		public int getCmt()
		{
			return ((pad0 & 0x3FFFFFFF) << 2) | ((clampt & 1) << 1) | (mirrort & 1);
		}

		public void setCms(int value)
		{
			pad1 = (value >> 2) & 0x3FFFFFFF;
			clamps = (value >> 1) & 1;
			mirrors = (value) & 1;
		}

		public int getCms()
		{
			return ((pad1 & 0x3FFFFFFF) << 2) | ((clamps & 1) << 1) | (mirrors & 1);
		}

		public final int powof(int dim)
		{
			int num = 1;
			int i = 0;
			while (num < dim)
			{
				num <<= 1;
				i++;
			}
			return i;
		}
	};

	private static interface InterleaveFunc
	{
		public void Interleave(byte[] mem, int off, int numDWords);
	};

	private final InterleaveFunc DWordInterleave = new InterleaveFunc()
	{
		public final void Interleave(byte[] mem, int off, int numDWords)
		{
			numDWords = off + (numDWords << 3);
			for (int i = off; i < numDWords; i += 8)
			{
				System.arraycopy(mem, i, tmp, 0, 4);
				System.arraycopy(mem, i + 4, mem, i, 4);
				System.arraycopy(tmp, 0, mem, i + 4, 4);
			}
		}
	};

	private final InterleaveFunc QWordInterleave = new InterleaveFunc()
	{
		public final void Interleave(byte[] mem, int off, int numDWords)
		{
			numDWords = off + (numDWords << 3);
			for (int i = off; i < numDWords; i += 16)
			{
				System.arraycopy(mem, i, tmp, 0, 4);
				System.arraycopy(mem, i + 8, mem, i, 4);
				System.arraycopy(tmp, 0, mem, i + 8, 4);
				System.arraycopy(mem, i + 4, tmp, 0, 4);
				System.arraycopy(mem, i + 12, mem, i + 4, 4);
				System.arraycopy(tmp, 0, mem, i + 12, 4);
			}
		}
	};

	// used by OpenGlGdp.updateStates
	public int changed;
	// called by OpenGlGdp.drawTexturedRectangle
	public gDPTile[] textureTile = new gDPTile[2];
	public CachedTexture[] current = new CachedTexture[2];

	private boolean ARB_multitexture; // TNT, GeForce, Rage 128, Radeon
	private int cachedBytes;
	private CachedTextureStack stack = new CachedTextureStack();
	private Texture texture = new Texture();
	private gDPTile[] tiles = new gDPTile[8];
	private gDPTile loadTile = new gDPTile();
	private TexRect texRect = new TexRect();
	private TextureImage textureImage = new TextureImage();
	private int loadType;
	private ByteBuffer paletteCRC16 = ByteBuffer.allocate(64);
	private ByteBuffer paletteCRC256 = ByteBuffer.allocate(4);
	private int textureMode;
	private GL2 gl;
	private int maxBytes;
	private int textureBitDepth;
	private final byte[] tmp = new byte[4];
	private int hits;
	private int misses;
	private int[] glNoiseNames = new int[32];
	private CachedTexture dummy;
	private BgImage bgImage = new BgImage();
	private Checksum crc32;
	private Bus16bit rdram;
	private ByteBuffer tmem;
	private int rdramSize;
	private boolean enable2xSaI;
	private int[] crcTable = new int[256];
	private int maxTextureUnits; // TNT = 2, GeForce = 2-4, Rage 128 = 2, Radeon = 3-6

	// called by OpenGl
	public TextureCache()
	{
	}

	// called by Gdp.constructor
	public void construct()
	{
		for (int i = 0; i < tiles.length; i++)
			tiles[i] = new gDPTile();
		for (int i = 0; i < textureTile.length; i++)
			textureTile[i] = new gDPTile();
		loadTile = tiles[7];
		textureTile[0] = tiles[0];
		textureTile[1] = tiles[1];
	}

	// called by OpenGlGdp.config
	public void config(int maxBytes, int textureBitDepth)
	{
		this.maxBytes = maxBytes;
		this.textureBitDepth = textureBitDepth;
		enable2xSaI = false;
	}

	// called by OpenGlGdp.init
	public void init(GL2 gl, Hardware rdram, ByteBuffer tmem, int maxTextureUnits, boolean ARB_multitexture)
	{
		this.gl = gl;
		this.rdram = (Bus16bit) rdram;
		this.tmem = tmem;
		this.maxTextureUnits = maxTextureUnits;
		this.ARB_multitexture = ARB_multitexture;
		ImageFormat.settMem(tmem);
		this.rdramSize = ((Bus32bit) rdram).read32bit(0x03F00028);
		ByteBuffer dummyTexture = ByteBuffer.allocateDirect(16 * 4);

		current[0] = null;
		current[1] = null;
		stack.init(gl);
		cachedBytes = 0;

		gl.glGenTextures(32, glNoiseNames, 0);

		ByteBuffer noise = ByteBuffer.allocateDirect(64 * 64 * 4);
		for (int i = 0; i < 32; i++)
		{
			gl.glBindTexture(GL2.GL_TEXTURE_2D, glNoiseNames[i]);

			Random rand = new Random();

			for (int y = 0; y < 64; y++)
			{
				for (int x = 0; x < 64; x++)
				{
					byte random = (byte) (rand.nextInt() & 0xFF);
					noise.put(y * 64 * 4 + x * 4, random);
					noise.put(y * 64 * 4 + x * 4 + 1, random);
					noise.put(y * 64 * 4 + x * 4 + 2, random);
					noise.put(y * 64 * 4 + x * 4 + 3, random);
				}
			}
			gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA8, 64, 64, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, noise);
		}

		dummy = CachedTexture.getDummy();
		prune();
		stack.addTop(dummy);

		gl.glBindTexture(GL2.GL_TEXTURE_2D, dummy.glName[0]);
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA8, 2, 2, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, dummyTexture);

		cachedBytes = dummy.textureBytes;

		crc32 = new Adler32();

		for (int i = 0; i < maxTextureUnits; i++)
			activateDummy(i);

		crcBuildTable();
	}

	// called by OpenGl.addTriangle
	public float getTexS(int tex, float scale)
	{
		return (scale * current[tex].shiftScaleS * texture.scales - textureTile[0].fuls + current[tex].offsetS) * current[tex].scaleS;
	}

	// called by OpenGl.addTriangle
	public float getTexT(int tex, float scale)
	{
		return (scale * current[tex].shiftScaleT * texture.scalet - textureTile[tex].fult + current[tex].offsetT) * current[tex].scaleT;
	}

	// called by OpenGlGdp.updateStates
	public void activateDummy(int w1)
	{
		if (ARB_multitexture)
			gl.glActiveTexture(GL2.GL_TEXTURE0 + w1);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, dummy.glName[0]);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
	}

	public void setTexture(int w1, int w2)
	{
		// setTexture(
		// ((w2 >> 16) & 0xFFFF) * 1.5258789e-05f,
		// ((w2 >> 0) & 0xFFFF) * 1.5258789e-05f,
		// (w1 >> 16) & 0xFF,
		// (w1 >> 8) & 0xFF,
		// (w1 >> 0) & 0xFF);
		// float sc = ((w2 >> 16) & 0xFFFF) * 1.5258789e-05f;
		// float tc = ((w2 >> 0) & 0xFFFF) * 1.5258789e-05f;
		// int level = (w1 >> 16) & 0xFF;
		// int tile = (w1 >> 8) & 0xFF;
		// int on = (w1 >> 0) & 0xFF;

		texture.scales = ((w2 >> 16) & 0xFFFF) * 1.5258789e-05f;
		texture.scalet = ((w2 >> 0) & 0xFFFF) * 1.5258789e-05f;
		// TODO - SHOULD BE THIS:
		// texture.scales = (((w2 >> 16) & 0xFFFF) + 1) / 65536.0f;
		// texture.scalet = (((w2 >> 0) & 0xFFFF) + 1) / 65536.0f;
		// texture.scales /= 32.0f;
		// texture.scalet /= 32.0f;

		if (texture.scales == 0.0f)
			texture.scales = 1.0f;
		if (texture.scalet == 0.0f)
			texture.scalet = 1.0f;
		texture.level = (w1 >> 16) & 0xFF;
		texture.on = (w1 >> 0) & 0xFF;
		texture.tile = (w1 >> 8) & 0xFF;

		textureTile[0] = tiles[texture.tile];
		textureTile[1] = tiles[(texture.tile < 7) ? (texture.tile + 1) : texture.tile];
	}

	// called by Gdp.gDPTexture
	// private void setTexture(float sc, float tc, int level, int tile, int on) {
	// texture.scales = sc;
	// texture.scalet = tc;
	// if (texture.scales == 0.0f)
	// texture.scales = 1.0f;
	// if (texture.scalet == 0.0f)
	// texture.scalet = 1.0f;
	// texture.level = level;
	// texture.on = on;
	// texture.tile = tile;
	// textureTile[0] = tiles[tile];
	// textureTile[1] = tiles[(tile < 7) ? (tile + 1) : tile];
	// }

	public void setTextureImage(int w1, int w2)
	{
		// setTextureImage(
		// (w1 >> 21) & 0x7,
		// (w1 >> 19) & 0x3,
		// (w1 & 0xFFF) + 1,
		// w2 & 0x3FFFFFF);

		textureImage.format = (w1 >> 21) & 0x7;
		textureImage.size = (w1 >> 19) & 0x3;
		textureImage.width = (w1 & 0xFFF) + 1;
		textureImage.address = w2 & 0x3FFFFFF;
		textureImage.bpl = textureImage.width << textureImage.size >> 1;
	}

	// called by Gdp.gDPSetTextureImage
	// private void setTextureImage(int format, int size, int width, int address) {
	// textureImage.format = format;
	// textureImage.size = size;
	// textureImage.width = width;
	// textureImage.address = address;
	// textureImage.bpl = textureImage.width << textureImage.size >> 1;
	// }

	public void setTextureTile(int w1, int w2, int w3, int w4, int w5)
	{
		float ulx = ((w2 >> 12) & 0xFFF) * 0.25f;
		float uly = ((w2 >> 0) & 0xFFF) * 0.25f;
		float lrx = ((w1 >> 12) & 0xFFF) * 0.25f;
		float lry = ((w1 >> 0) & 0xFFF) * 0.25f;
		int tile = (w2 >> 24) & 0x7;
		float dsdx = ((w4 >> 16) & 0xFFFF) * 0.0009765625f;
		float dtdy = ((w4 >> 0) & 0xFFFF) * 0.0009765625f;
		float s = ((w3 >> 16) & 0xFFFF) * 0.03125f;
		float t = ((w3 >> 0) & 0xFFFF) * 0.03125f;

		if (((w5 >> 20) & 0x3) == 2)
		{
			dsdx = 1.0f;
			lrx += 1.0f;
			lry += 1.0f;
		}

		textureTile[0] = tiles[tile];
		textureTile[1] = tiles[tile < 7 ? tile + 1 : tile];

		if (textureMode == TEXTUREMODE_NORMAL)
			textureMode = TEXTUREMODE_TEXRECT;

		texRect.width = (int) (StrictMath.max(s + (lrx - ulx - 1) * dsdx, s) + dsdx);
		texRect.height = (int) (StrictMath.max(t + (lry - uly - 1) * dtdy, t) + dtdy);
	}

	public void setTextureTileFlip(int w1, int w2, int w3, int w4, int w5)
	{
		float ulx = ((w2 >> 12) & 0xFFF) * 0.25f;
		float uly = ((w2 >> 0) & 0xFFF) * 0.25f;
		float lrx = ((w1 >> 12) & 0xFFF) * 0.25f;
		float lry = ((w1 >> 0) & 0xFFF) * 0.25f;
		int tile = (w2 >> 24) & 0x7;
		float dsdx = -(((w4 >> 16) & 0xFFFF) * 0.0009765625f);
		float dtdy = -(((w4 >> 0) & 0xFFFF) * 0.0009765625f);
		float s = (((w3 >> 16) & 0xFFFF) * 0.03125f) + (lrx - ulx) * dsdx;
		float t = (((w3 >> 0) & 0xFFFF) * 0.03125f) + (lry - uly) * dtdy;

		if (((w5 >> 20) & 0x3) == 2)
		{
			dsdx = 1.0f;
			lrx += 1.0f;
			lry += 1.0f;
		}

		textureTile[0] = tiles[tile];
		textureTile[1] = tiles[tile < 7 ? tile + 1 : tile];

		if (textureMode == TEXTUREMODE_NORMAL)
			textureMode = TEXTUREMODE_TEXRECT;

		texRect.width = (int) (StrictMath.max(s + (lrx - ulx - 1) * dsdx, s) + dsdx);
		texRect.height = (int) (StrictMath.max(t + (lry - uly - 1) * dtdy, t) + dtdy);
	}

	// called by Gdp.gDPTextureRectangle
	// public void setTextureTile(float lrs, float lrt, int tile, float s, float t, float dsdx, float dtdy) {
	// textureTile[0] = tiles[tile];
	// textureTile[1] = tiles[tile < 7 ? tile + 1 : tile];
	//
	// if (textureMode == TEXTUREMODE_NORMAL)
	// textureMode = TEXTUREMODE_TEXRECT;
	//
	// texRect.width = (int)(StrictMath.max(lrs, s) + dsdx);
	// texRect.height = (int)(StrictMath.max(lrt, t) + dtdy);
	// }

	// called by Gdp.gDPTextureRectangle
	public void resetTextureTile()
	{
		textureTile[0] = tiles[texture.tile];
		textureTile[1] = tiles[texture.tile < 7 ? texture.tile + 1 : texture.tile];
	}

	public void setTile(int w1, int w2)
	{
		// setTile(
		// (w1 >> 21) & 0x7,
		// (w1 >> 19) & 0x3,
		// (w1 >> 9) & 0x1FF,
		// (w1 >> 0) & 0x1FF,
		// (w2 >> 24) & 0x7,
		// (w2 >> 20) & 0xF,
		// (w2 >> 18) & 0x3,
		// (w2 >> 8) & 0x3,
		// (w2 >> 14) & 0xF,
		// (w2 >> 4) & 0xF,
		// (w2 >> 10) & 0xF,
		// (w2 >> 0) & 0xF);
		int format = (w1 >> 21) & 0x7;
		int size = (w1 >> 19) & 0x3;
		int tile = (w2 >> 24) & 0x7;

		if (((size == G_IM_SIZ_4b) || (size == G_IM_SIZ_8b)) && (format == G_IM_FMT_RGBA))
			format = G_IM_FMT_CI;

		tiles[tile].format = format;
		tiles[tile].size = size;
		tiles[tile].line = (w1 >> 9) & 0x1FF;
		tiles[tile].tmem = (w1 >> 0) & 0x1FF;
		tiles[tile].palette = (w2 >> 20) & 0xF;
		tiles[tile].setCmt((w2 >> 18) & 0x3);
		tiles[tile].setCms((w2 >> 8) & 0x3);
		tiles[tile].maskt = (w2 >> 14) & 0xF;
		tiles[tile].masks = (w2 >> 4) & 0xF;
		tiles[tile].shiftt = (w2 >> 10) & 0xF;
		tiles[tile].shifts = (w2 >> 0) & 0xF;
		if (tiles[tile].masks == 0)
			tiles[tile].clamps = 1;
		if (tiles[tile].maskt == 0)
			tiles[tile].clampt = 1;
	}

	// called by Gdp.gDPSetTile
	// private void setTile(int format, int size, int line, int tmem, int tile, int palette, int cmt, int cms, int maskt, int masks, int shiftt, int shifts) {
	// if (((size == G_IM_SIZ_4b) || (size == G_IM_SIZ_8b)) && (format == G_IM_FMT_RGBA))
	// format = G_IM_FMT_CI;
	//
	// tiles[tile].format = format;
	// tiles[tile].size = size;
	// tiles[tile].line = line;
	// tiles[tile].tmem = tmem;
	// tiles[tile].palette = palette;
	// tiles[tile].setCmt(cmt);
	// tiles[tile].setCms(cms);
	// tiles[tile].maskt = maskt;
	// tiles[tile].masks = masks;
	// tiles[tile].shiftt = shiftt;
	// tiles[tile].shifts = shifts;
	// if (tiles[tile].masks==0)
	// tiles[tile].clamps = 1;
	// if (tiles[tile].maskt==0)
	// tiles[tile].clampt = 1;
	// }

	public void setTileSize(int w1, int w2)
	{
		// setTileSize(
		// (w2 >> 24) & 0x7,
		// (w1 >> 12) & 0xFFF,
		// (w1 >> 0) & 0xFFF,
		// (w2 >> 12) & 0xFFF,
		// (w2 >> 0) & 0xFFF);
		int tile = (w2 >> 24) & 0x7;
		int uls = (w1 >> 12) & 0xFFF;
		int ult = (w1 >> 0) & 0xFFF;
		int lrs = (w2 >> 12) & 0xFFF;
		int lrt = (w2 >> 0) & 0xFFF;

		tiles[tile].uls = (uls >> 2) & 0x3ff;
		tiles[tile].ult = (ult >> 2) & 0x3ff;
		tiles[tile].lrs = (lrs >> 2) & 0x3ff;
		tiles[tile].lrt = (lrt >> 2) & 0x3ff;
		tiles[tile].fuls = uls * 0.25f; // uls / 4
		tiles[tile].fult = ult * 0.25f; // ult / 4
		tiles[tile].flrs = lrs * 0.25f; // lrs / 4
		tiles[tile].flrt = lrt * 0.25f; // lrt / 4
	}

	// called by Gdp.gDPSetTileSize
	// private void setTileSize(int tile, int uls, int ult, int lrs, int lrt) {
	// tiles[tile].uls = (uls >> 2) & 0x3ff;
	// tiles[tile].ult = (ult >> 2) & 0x3ff;
	// tiles[tile].lrs = (lrs >> 2) & 0x3ff;
	// tiles[tile].lrt = (lrt >> 2) & 0x3ff;
	// tiles[tile].fuls = uls * 0.25f; // uls / 4
	// tiles[tile].fult = ult * 0.25f; // ult / 4
	// tiles[tile].flrs = lrs * 0.25f; // lrs / 4
	// tiles[tile].flrt = lrt * 0.25f; // lrt / 4
	// }

	public void loadTile(int w1, int w2)
	{
		// loadTile((w2 >> 24) & 0x7);

		InterleaveFunc Interleave;
		int line;

		loadTile = tiles[(w2 >> 24) & 0x7];

		if (loadTile.line == 0)
			return;

		int address = textureImage.address + loadTile.ult * textureImage.bpl + (loadTile.uls << textureImage.size >> 1);
		int dest = loadTile.tmem << 3;
		int bpl = (loadTile.lrs - loadTile.uls + 1) << loadTile.size >> 1;
		int height = loadTile.lrt - loadTile.ult + 1;
		int src = address;

		// Stay within TMEM
		if (((address + height * bpl) > rdramSize) || (((loadTile.tmem << 3) + bpl * height) > 4096))
			return;

		byte[] tmemArray = tmem.array();

		// Line given for 32-bit is half what it seems it should since they split the
		// high and low words. I'm cheating by putting them together.
		if (loadTile.size == G_IM_SIZ_32b)
		{
			line = loadTile.line << 1;
			Interleave = QWordInterleave;
		}
		else
		{
			line = loadTile.line;
			Interleave = DWordInterleave;
		}

		int bplTex = textureImage.bpl;
		int lineBytes = line << 3;
		for (int y = 0; y < height; y++)
		{
			((BusDMA) rdram).readDMA(src, tmem, dest, bpl);
			
			if ((y & 1) != 0)
			{
				Interleave.Interleave(tmemArray, dest, line);
			}

			src += bplTex;
			dest += lineBytes;
		}

		textureMode = TEXTUREMODE_NORMAL;
		loadType = LOADTYPE_TILE;
		changed |= CHANGED_TMEM;
	}

	// called by Gdp.gDPLoadTile
	// private void loadTile(int tile) {
	// InterleaveFunc Interleave;
	// int line;
	//
	// loadTile = tiles[tile];
	//
	// if (loadTile.line == 0)
	// return;
	//
	// int address = textureImage.address + loadTile.ult * textureImage.bpl + (loadTile.uls << textureImage.size >> 1);
	// int dest = loadTile.tmem << 3;
	// int bpl = (loadTile.lrs - loadTile.uls + 1) << loadTile.size >> 1;
	// int height = loadTile.lrt - loadTile.ult + 1;
	// int src = address;
	//
	// // Stay within TMEM
	// if (((address + height * bpl) > rdramSize) || (((loadTile.tmem << 3) + bpl * height) > 4096))
	// return;
	//
	// byte[] tmemArray = tmem.array();
	//
	// // Line given for 32-bit is half what it seems it should since they split the
	// // high and low words. I'm cheating by putting them together.
	// if (loadTile.size == G_IM_SIZ_32b) {
	// line = loadTile.line << 1;
	// Interleave = QWordInterleave;
	// } else {
	// line = loadTile.line;
	// Interleave = DWordInterleave;
	// }
	//
	// int bplTex = textureImage.bpl;
	// int lineBytes = line << 3;
	// for (int y = 0; y < height; y++) {
	// try {
	// rdram.loadDma(src, tmemArray, dest, bpl);
	// } catch(MemoryException e) {
	// e.printStackTrace();
	// }
	// if ((y & 1)!=0) {
	// Interleave.Interleave(tmemArray, dest, line);
	// }
	//
	// src += bplTex;
	// dest += lineBytes;
	// }
	//
	// textureMode = TEXTUREMODE_NORMAL;
	// loadType = LOADTYPE_TILE;
	// changed |= CHANGED_TMEM;
	// }

	public void loadBlock(int w1, int w2)
	{
		// loadBlock(
		// (w2 >> 24) & 0x7,
		// (w1 >> 12) & 0xFFF,
		// (w1 >> 0) & 0xFFF,
		// (w2 >> 12) & 0xFFF,
		// (w2 >> 0) & 0xFFF);

		loadTile = tiles[(w2 >> 24) & 0x7];

		int bytes = (((w2 >> 12) & 0xFFF) + 1) << loadTile.size >>> 1;
		int address = textureImage.address + ((w1 >> 0) & 0xFFF) * textureImage.bpl + (((w1 >> 12) & 0xFFF) << textureImage.size >>> 1);

		if ((bytes == 0) ||
				((address + bytes) > rdramSize) ||
				(((loadTile.tmem << 3) + bytes) > 4096)) { return; }

		int src = address;
		int dest = loadTile.tmem << 3;

		byte[] tmemArray = tmem.array();

		int dxt = (w2 >> 0) & 0xFFF;

		if (dxt > 0)
		{
			InterleaveFunc Interleave;

			int line = (2047 + dxt) / dxt;
			int bpl = line << 3;
			int height = bytes / bpl;

			if (loadTile.size == G_IM_SIZ_32b)
				Interleave = QWordInterleave;
			else
				Interleave = DWordInterleave;

			for (int y = 0; y < height; y++)
			{
				((BusDMA) rdram).readDMA(src, tmem, dest, bpl);
				
				if ((y & 1) != 0)
				{
					Interleave.Interleave(tmemArray, dest, line);
				}

				src += bpl;
				dest += bpl;
			}
		}
		else
		{
			((BusDMA) rdram).readDMA(src, tmem, dest, bytes);
		}

		textureMode = TEXTUREMODE_NORMAL;
		loadType = LOADTYPE_BLOCK;
		changed |= CHANGED_TMEM;
	}

	// called by Gdp.gDPLoadBlock
	// private void loadBlock(int tile, int uls, int ult, int lrs, int dxt) {
	// loadTile = tiles[tile];
	//
	// int bytes = (lrs + 1) << loadTile.size >>> 1;
	// int address = textureImage.address + ult * textureImage.bpl + (uls << textureImage.size >>> 1);
	//
	// if ((bytes == 0) ||
	// ((address + bytes) > rdramSize) ||
	// (((loadTile.tmem << 3) + bytes) > 4096)) {
	// return;
	// }
	//
	// int src = address;
	// int dest = loadTile.tmem << 3;
	//
	// byte[] tmemArray = tmem.array();
	//
	// if (dxt > 0) {
	// InterleaveFunc Interleave;
	//
	// int line = (2047 + dxt) / dxt;
	// int bpl = line << 3;
	// int height = bytes / bpl;
	//
	// if (loadTile.size == G_IM_SIZ_32b)
	// Interleave = QWordInterleave;
	// else
	// Interleave = DWordInterleave;
	//
	// for (int y = 0; y < height; y++) {
	// try {
	// rdram.loadDma(src, tmemArray, dest, bpl);
	// } catch(MemoryException e) {
	// e.printStackTrace();
	// }
	// if ((y & 1)!=0) {
	// Interleave.Interleave(tmemArray, dest, line);
	// }
	//
	// src += bpl;
	// dest += bpl;
	// }
	// } else {
	// try {
	// rdram.loadDma(src, tmemArray, dest, bytes);
	// } catch(MemoryException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// textureMode = TEXTUREMODE_NORMAL;
	// loadType = LOADTYPE_BLOCK;
	// changed |= CHANGED_TMEM;
	// }

	public void loadLUT(int w1, int w2)
	{
		// loadLUT((w2 >> 24) & 0x7);
		int tile = (w2 >> 24) & 0x7;
		int count = ((tiles[tile].lrs - tiles[tile].uls + 1) * (tiles[tile].lrt - tiles[tile].ult + 1)) & 0xFFFF;
		int address = textureImage.address + tiles[tile].ult * textureImage.bpl + (tiles[tile].uls << textureImage.size >>> 1);
		int dest = tiles[tile].tmem * 8;
		int pal = ((tiles[tile].tmem - 256) >>> 4) & 0xFFFF;

		tmem.position(0);
		ByteBuffer tmemPtr = tmem.slice();
		paletteCRC16.position(0);

		int i = 0;
		while (i < count)
		{
			for (int j = 0; (j < 16) && (i < count); j++, i++)
			{
				tmem.putShort(dest, rdram.read16bit(address + (i * 2)));
				dest += 8;
			}

			tmemPtr.position((256 + (pal << 4)) * 8);
			paletteCRC16.asIntBuffer().put(pal, crcCalculatePalette(0xFFFFFFFF, tmemPtr.slice(), 16));
			pal++;
		}

		crc32.reset();
		crc32.update(paletteCRC16.array(), 0, 64);
		paletteCRC256.asIntBuffer().put(0, (int) crc32.getValue());
		changed |= CHANGED_TMEM;
	}

	// called by Gdp.gDPLoadTLUT
	// private void loadLUT(int tile) {
	// int count = ((tiles[tile].lrs - tiles[tile].uls + 1) * (tiles[tile].lrt - tiles[tile].ult + 1))&0xFFFF;
	// int address = textureImage.address + tiles[tile].ult * textureImage.bpl + (tiles[tile].uls << textureImage.size >>> 1);
	// int dest = tiles[tile].tmem*8;
	// int pal = ((tiles[tile].tmem - 256) >>> 4)&0xFFFF;
	//
	// tmem.position(0);
	// ByteBuffer tmemPtr = tmem.slice();
	// paletteCRC16.position(0);
	//
	// int i = 0;
	// while (i < count) {
	// try {
	// for (int j = 0; (j < 16) && (i < count); j++, i++) {
	// tmem.putShort(dest, rdram.loadHalfWord(address + (i*2)));
	// dest += 8;
	// }
	// } catch(MemoryException e) {
	// e.printStackTrace();
	// }
	//
	// tmemPtr.position((256 + (pal << 4))*8);
	// paletteCRC16.asIntBuffer().put(pal, crcCalculatePalette(0xFFFFFFFF, tmemPtr.slice(), 16));
	// pal++;
	// }
	//
	// crc32.reset();
	// crc32.update(paletteCRC16.array(), 0, 64);
	// paletteCRC256.asIntBuffer().put(0, (int)crc32.getValue());
	// changed |= CHANGED_TMEM;
	// }

	// called by OpenGlGdp.updateStates
	public void update(int w1, int w2)
	{
		int tex = w1;
		boolean IA16 = ((w2 >> 14) & 0x3) == 3;
		boolean linear = (((w2 >> 12) & 0x3) == 2) || (((w2 >> 12) & 0x3) == 3);
		int maxTexels;
		int tileWidth;
		int maskWidth;
		int loadWidth;
		int lineWidth;
		int clampWidth;
		int height;
		int tileHeight;
		int maskHeight;
		int loadHeight;
		int lineHeight;
		int clampHeight;
		int width;

		if (textureMode == TEXTUREMODE_BGIMAGE)
		{
			updateBackground(IA16, linear);
			return;
		}

		maxTexels = ImageFormat.imageFormat[textureTile[tex].size][textureTile[tex].format].maxTexels;

		// Here comes a bunch of code that just calculates the texture size...I wish there was an easier way...
		tileWidth = textureTile[tex].lrs - textureTile[tex].uls + 1;
		tileHeight = textureTile[tex].lrt - textureTile[tex].ult + 1;

		maskWidth = 1 << textureTile[tex].masks;
		maskHeight = 1 << textureTile[tex].maskt;

		loadWidth = loadTile.lrs - loadTile.uls + 1;
		loadHeight = loadTile.lrt - loadTile.ult + 1;

		lineWidth = textureTile[tex].line << ImageFormat.imageFormat[textureTile[tex].size][textureTile[tex].format].lineShift;

		if (lineWidth != 0) // Don't allow division by zero
			lineHeight = StrictMath.min(maxTexels / lineWidth, tileHeight);
		else
			lineHeight = 0;

		if (textureMode == TEXTUREMODE_TEXRECT)
		{
			int texRectWidth = texRect.width - textureTile[tex].uls;
			int texRectHeight = texRect.height - textureTile[tex].ult;

			if ((textureTile[tex].masks != 0) && ((maskWidth * maskHeight) <= maxTexels))
				width = maskWidth;
			else if ((tileWidth * tileHeight) <= maxTexels)
				width = tileWidth;
			else if ((tileWidth * texRectHeight) <= maxTexels)
				width = tileWidth;
			else if ((texRectWidth * tileHeight) <= maxTexels)
				width = texRect.width;
			else if ((texRectWidth * texRectHeight) <= maxTexels)
				width = texRect.width;
			else if (loadType == LOADTYPE_TILE)
				width = loadWidth;
			else
				width = lineWidth;

			if ((textureTile[tex].maskt != 0) && ((maskWidth * maskHeight) <= maxTexels))
				height = maskHeight;
			else if ((tileWidth * tileHeight) <= maxTexels)
				height = tileHeight;
			else if ((tileWidth * texRectHeight) <= maxTexels)
				height = texRect.height;
			else if ((texRectWidth * tileHeight) <= maxTexels)
				height = tileHeight;
			else if ((texRectWidth * texRectHeight) <= maxTexels)
				height = texRect.height;
			else if (loadType == LOADTYPE_TILE)
				height = loadHeight;
			else
				height = lineHeight;

		}
		else
		{
			if ((textureTile[tex].masks != 0) && ((maskWidth * maskHeight) <= maxTexels))
				width = maskWidth; // Use mask width if set and valid
			else if ((tileWidth * tileHeight) <= maxTexels)
				width = tileWidth; // else use tile width if valid
			else if (loadType == LOADTYPE_TILE)
				width = loadWidth; // else use load width if load done with LoadTile
			else
				width = lineWidth; // else use line-based width

			if ((textureTile[tex].maskt != 0) && ((maskWidth * maskHeight) <= maxTexels))
				height = maskHeight;
			else if ((tileWidth * tileHeight) <= maxTexels)
				height = tileHeight;
			else if (loadType == LOADTYPE_TILE)
				height = loadHeight;
			else
				height = lineHeight;
		}

		clampWidth = textureTile[tex].clamps != 0 ? tileWidth : width;
		clampHeight = textureTile[tex].clampt != 0 ? tileHeight : height;

		if (clampWidth > 256)
			textureTile[tex].clamps = 0;
		if (clampHeight > 256)
			textureTile[tex].clampt = 0;

		// Make sure masking is valid
		if (maskWidth > width)
		{
			textureTile[tex].masks = textureTile[tex].powof(width);
			maskWidth = 1 << textureTile[tex].masks;
		}

		if (maskHeight > height)
		{
			textureTile[tex].maskt = textureTile[tex].powof(height);
			maskHeight = 1 << textureTile[tex].maskt;
		}

		int src = textureTile[tex].tmem << 3;
		int bpl = width << textureTile[tex].size >> 1;
		int lineBytes = textureTile[tex].line << 3;
		crc32.reset();
		for (int y = 0; y < height; y++)
		{
			crc32.update(tmem.array(), src, bpl);
			src += lineBytes;
		}
		if (textureTile[tex].format == G_IM_FMT_CI)
		{
			if (textureTile[tex].size == G_IM_SIZ_4b)
			{
				crc32.update(paletteCRC16.array(), textureTile[tex].palette << 2, 4);
			}
			else if (textureTile[tex].size == G_IM_SIZ_8b)
			{
				crc32.update(paletteCRC256.array(), 0, 4);
			}
		}
		int crc = (int) crc32.getValue();

		CachedTexture texture = stack.top;
		while (texture != null)
		{
			if ((texture.crc == crc) &&
					(texture.width == width) &&
					(texture.height == height) &&
					(texture.clampWidth == clampWidth) &&
					(texture.clampHeight == clampHeight) &&
					(texture.maskS == textureTile[tex].masks) &&
					(texture.maskT == textureTile[tex].maskt) &&
					(texture.mirrorS == textureTile[tex].mirrors) &&
					(texture.mirrorT == textureTile[tex].mirrort) &&
					(texture.clampS == textureTile[tex].clamps) &&
					(texture.clampT == textureTile[tex].clampt) &&
					(texture.format == textureTile[tex].format) &&
					(texture.size == textureTile[tex].size))
			{
				activateTexture(tex, texture, linear);
				hits++;
				return;
			}

			texture = texture.lower;
		}

		misses++;

		// If multitexturing, set the appropriate texture
		if (ARB_multitexture)
			gl.glActiveTexture(GL2.GL_TEXTURE0 + tex);

		current[tex] = new CachedTexture();
		prune();
		stack.addTop(current[tex]);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, current[tex].glName[0]);

		current[tex].address = textureImage.address;
		current[tex].crc = crc;
		current[tex].format = textureTile[tex].format;
		current[tex].size = textureTile[tex].size;
		current[tex].width = width;
		current[tex].height = height;
		current[tex].clampWidth = clampWidth;
		current[tex].clampHeight = clampHeight;
		current[tex].palette = textureTile[tex].palette;
		current[tex].maskS = textureTile[tex].masks;
		current[tex].maskT = textureTile[tex].maskt;
		current[tex].mirrorS = textureTile[tex].mirrors;
		current[tex].mirrorT = textureTile[tex].mirrort;
		current[tex].clampS = textureTile[tex].clamps;
		current[tex].clampT = textureTile[tex].clampt;
		current[tex].line = textureTile[tex].line;
		current[tex].tMem = textureTile[tex].tmem;
		if (current[tex].clampS != 0)
			current[tex].realWidth = current[tex].pow2(clampWidth);
		else if (current[tex].mirrorS != 0)
			current[tex].realWidth = maskWidth << 1;
		else
			current[tex].realWidth = current[tex].pow2(width);
		if (current[tex].clampT != 0)
			current[tex].realHeight = current[tex].pow2(clampHeight);
		else if (current[tex].mirrorT != 0)
			current[tex].realHeight = maskHeight << 1;
		else
			current[tex].realHeight = current[tex].pow2(height);
		current[tex].scaleS = 1.0f / (float) (current[tex].realWidth);
		current[tex].scaleT = 1.0f / (float) (current[tex].realHeight);
		current[tex].shiftScaleS = 1.0f;
		current[tex].shiftScaleT = 1.0f;
		current[tex].offsetS = enable2xSaI ? 0.25f : 0.5f;
		current[tex].offsetT = enable2xSaI ? 0.25f : 0.5f;
		if (textureTile[tex].shifts > 10)
			current[tex].shiftScaleS = (float) (1 << (16 - textureTile[tex].shifts));
		else if (textureTile[tex].shifts > 0)
			current[tex].shiftScaleS /= (float) (1 << textureTile[tex].shifts);
		if (textureTile[tex].shiftt > 10)
			current[tex].shiftScaleT = (float) (1 << (16 - textureTile[tex].shiftt));
		else if (textureTile[tex].shiftt > 0)
			current[tex].shiftScaleT /= (float) (1 << textureTile[tex].shiftt);

		current[tex].load(IA16, textureBitDepth, tmem, gl);
		activateTexture(tex, current[tex], linear);
		cachedBytes += current[tex].textureBytes;
	}

	// Private Methods /////////////////////////////////////////////////////////

	// called by update()
	private void updateBackground(boolean IA16, boolean linear)
	{
		// calculate bgImage crc
		int numBytes = bgImage.width * bgImage.height << bgImage.size >>> 1;
		crc32.reset();
		ByteBuffer buff = ByteBuffer.allocate(numBytes);
		byte[] rdramcrc = buff.array();
		((BusDMA) rdram).readDMA(bgImage.address, buff, 0, numBytes);
		crc32.update(rdramcrc, 0, numBytes);
		if (bgImage.format == G_IM_FMT_CI)
		{
			if (bgImage.size == G_IM_SIZ_4b)
			{
				crc32.update(paletteCRC16.array(), bgImage.palette << 2, 4);
			}
			else if (bgImage.size == G_IM_SIZ_8b)
			{
				crc32.update(paletteCRC256.array(), 0, 4);
			}
		}
		int crc = (int) crc32.getValue();

		// find and activate bgImage
		CachedTexture tex = stack.top;
		while (tex != null)
		{
			if ((tex.crc == crc) &&
					(tex.width == bgImage.width) &&
					(tex.height == bgImage.height) &&
					(tex.format == bgImage.format) &&
					(tex.size == bgImage.size))
			{
				activateTexture(0, tex, linear);
				hits++;
				return;
			}
			tex = tex.lower;
		}

		// bgImage not found
		misses++;

		// If multitexturing, set the appropriate texture
		if (ARB_multitexture)
			gl.glActiveTexture(GL2.GL_TEXTURE0);

		current[0] = new CachedTexture();
		prune();
		stack.addTop(current[0]);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, current[0].glName[0]);

		current[0].address = bgImage.address;
		current[0].crc = crc;
		current[0].format = bgImage.format;
		current[0].size = bgImage.size;
		current[0].width = bgImage.width;
		current[0].height = bgImage.height;
		current[0].clampWidth = bgImage.width;
		current[0].clampHeight = bgImage.height;
		current[0].palette = bgImage.palette;
		current[0].maskS = 0;
		current[0].maskT = 0;
		current[0].mirrorS = 0;
		current[0].mirrorT = 0;
		current[0].clampS = 1;
		current[0].clampT = 1;
		current[0].line = 0;
		current[0].tMem = 0;
		current[0].realWidth = current[0].pow2(bgImage.width);
		current[0].realHeight = current[0].pow2(bgImage.height);
		current[0].scaleS = 1.0f / (float) (current[0].realWidth);
		current[0].scaleT = 1.0f / (float) (current[0].realHeight);
		current[0].shiftScaleS = 1.0f;
		current[0].shiftScaleT = 1.0f;

		current[0].loadBackground(IA16, textureBitDepth, (Hardware) rdram, gl, bgImage.width, bgImage.height, bgImage.size, bgImage.address);
		activateTexture(0, current[0], linear);
		cachedBytes += current[0].textureBytes;
	}

	private void prune()
	{
		while (cachedBytes > maxBytes)
		{
			if (stack.bottom != dummy)
			{
				cachedBytes -= stack.bottom.textureBytes;
				stack.removeBottom();
			}
			else if (dummy.higher != null)
			{
				stack.remove(dummy.higher);
				cachedBytes -= dummy.higher.textureBytes;
			}
			// may need else, can get stuck in this loop
		}
	}

	private void activateTexture(int tex, CachedTexture texture, boolean linear)
	{
		if (ARB_multitexture)
			gl.glActiveTexture(GL2.GL_TEXTURE0 + tex);
		// Bind the cached texture
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texture.glName[0]);
		// Set filter mode. Almost always bilinear, but check anyways
		if (linear)
		{
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		}
		else
		{
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
		}
		// Set clamping modes
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, texture.clampS != 0 ? GL2.GL_CLAMP_TO_EDGE : GL2.GL_REPEAT);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, texture.clampT != 0 ? GL2.GL_CLAMP_TO_EDGE : GL2.GL_REPEAT);
		stack.moveToTop(texture);
		current[tex] = texture;
	}

	// called by loadLUT
	private int crcCalculatePalette(int crc, ByteBuffer buffer, int count)
	{
		int p = 0;
		int orig = crc;

		while ((count--) != 0)
		{
			crc = (crc >>> 8) ^ crcTable[(crc & 0xFF) ^ (buffer.get(p++) & 0xFF)];
			crc = (crc >>> 8) ^ crcTable[(crc & 0xFF) ^ (buffer.get(p++) & 0xFF)];
			p += 6;
		}
		return crc ^ orig;
	}

	// called by init
	private void crcBuildTable()
	{
		int crc;

		for (int i = 0; i <= 255; i++)
		{
			crc = reflect(i, 8) << 24;
			for (int j = 0; j < 8; j++)
				crc = (crc << 1) ^ (((crc & (1 << 31)) != 0) ? CRC32_POLYNOMIAL : 0);

			crcTable[i] = reflect(crc, 32);
		}
	}

	private int reflect(int ref, int ch)
	{
		int value = 0;
		// Swap bit 0 for bit 7
		// bit 1 for bit 6, etc.
		for (int i = 1; i < (ch + 1); i++)
		{
			if ((ref & 1) != 0)
				value |= (1 << (ch - i));
			ref = ref >> 1;
		}
		return value;
	}
}