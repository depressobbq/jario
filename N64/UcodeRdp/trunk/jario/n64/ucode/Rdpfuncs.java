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

import java.nio.ByteBuffer;

import jario.hardware.Bus32bit;
import jario.hardware.Bus8bit;
import jario.hardware.Hardware;
import jario.n64.ucode.F3d;
import jario.n64.ucode.Gbi;

public class Rdpfuncs extends F3d
{
	protected static ByteBuffer cmdbuf = ByteBuffer.allocate(16);
	protected static byte[] cmd = cmdbuf.array();

	public Rdpfuncs()
	{
	}

	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0: // not used
			break;
		case 1:
			rdram = (Bus8bit) bus;
			break;
		case 2:
			sp = (Bus32bit) bus;
			break;
		case 3:
			dp = (Bus32bit) bus;
			break;
		}
	}

	public void reset()
	{
	}

	@Override
	public void clock(long ticks)
	{
		for (int i = 0; i <= 0xFF; i++)
			dlist[i] = gbiUnknown;

		for (int i = 0xC8; i <= 0xCF; i++)
			dlist[i] = RDP_Unknown;
		for (int i = 0xE4; i <= 0xFF; i++)
			dlist[i] = RDP_Unknown;

		dlist[0x00] = RDP_NoOp;
		dlist[0xFF] = RDP_SetCImg;
		dlist[0xFE] = RDP_SetZImg;
		dlist[0xFD] = RDP_SetTImg;
		dlist[0xFC] = RDP_SetCombine;
		dlist[0xFB] = RDP_SetEnvColor;
		dlist[0xFA] = RDP_SetPrimColor;
		dlist[0xF9] = RDP_SetBlendColor;
		dlist[0xF8] = RDP_SetFogColor;
		dlist[0xF7] = RDP_SetFillColor;
		dlist[0xF6] = RDP_FillRect;
		dlist[0xF5] = RDP_SetTile;
		dlist[0xF4] = RDP_LoadTile;
		dlist[0xF3] = RDP_LoadBlock;
		dlist[0xF2] = RDP_SetTileSize;
		dlist[0xF0] = RDP_LoadTLUT;
		dlist[0xEF] = RDP_SetOtherMode;
		dlist[0xEE] = RDP_SetPrimDepth;
		dlist[0xED] = RDP_SetScissor;
		dlist[0xEC] = RDP_SetConvert;
		dlist[0xEB] = RDP_SetKeyR;
		dlist[0xEA] = RDP_SetKeyGB;
		dlist[0xE9] = RDP_FullSync;
		dlist[0xE8] = RDP_TileSync;
		dlist[0xE7] = RDP_PipeSync;
		dlist[0xE6] = RDP_LoadSync;
		dlist[0xE5] = RDP_TexRectFlip;
		dlist[0xE4] = RDP_TexRect;
	}

	public int readRegister(int reg)
	{
		return 0;
	}

	public void writeRegister(int reg, int value)
	{

	}

	/************************* OpCode functions *************************/

	protected OpCode RDP_Unknown = new OpCode()
	{
		public void exec(int w1, int w2)
		{
		}
	};

	protected OpCode RDP_NoOp = new OpCode()
	{
		public void exec(int w1, int w2)
		{
		}
	};

	protected OpCode RDP_SetCImg = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			int addr = segmentToPhysical(w2);
			cmd[0] = Gbi.G_SETCIMG;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (addr >> 24);
			cmd[5] = (byte) (addr >> 16);
			cmd[6] = (byte) (addr >> 8);
			cmd[7] = (byte) (addr >> 0);
			dpDMA.writeDMA(Gbi.G_SETCIMG, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetZImg = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			updateStates();
			int addr = segmentToPhysical(w2);
			cmd[0] = Gbi.G_SETZIMG;
			cmd[4] = (byte) (addr >> 24);
			cmd[5] = (byte) (addr >> 16);
			cmd[6] = (byte) (addr >> 8);
			cmd[7] = (byte) (addr >> 0);
			dpDMA.writeDMA(Gbi.G_SETZIMG, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetTImg = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			int addr = segmentToPhysical(w2);
			cmd[0] = Gbi.G_SETTIMG;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (addr >> 24);
			cmd[5] = (byte) (addr >> 16);
			cmd[6] = (byte) (addr >> 8);
			cmd[7] = (byte) (addr >> 0);
			dpDMA.writeDMA(Gbi.G_SETTIMG, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetCombine = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETCOMBINE;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETCOMBINE, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetEnvColor = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETENVCOLOR;
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETENVCOLOR, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetPrimColor = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETPRIMCOLOR;
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETPRIMCOLOR, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetBlendColor = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETBLENDCOLOR;
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETBLENDCOLOR, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetFogColor = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETFOGCOLOR;
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETFOGCOLOR, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetFillColor = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETFILLCOLOR;
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETFILLCOLOR, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_FillRect = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			updateStates();
			cmd[0] = Gbi.G_FILLRECT;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_FILLRECT, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetTile = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETTILE;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETTILE, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_LoadTile = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_LOADTILE;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_LOADTILE, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_LoadBlock = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_LOADBLOCK;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_LOADBLOCK, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetTileSize = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETTILESIZE;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETTILESIZE, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_LoadTLUT = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_LOADTLUT;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_LOADTLUT, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetOtherMode = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_RDPSETOTHERMODE;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_RDPSETOTHERMODE, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetPrimDepth = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETPRIMDEPTH;
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETPRIMDEPTH, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetScissor = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETSCISSOR;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETSCISSOR, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetConvert = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETCONVERT;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETCONVERT, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetKeyR = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETKEYR;
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETKEYR, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_SetKeyGB = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_SETKEYGB;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			dpDMA.writeDMA(Gbi.G_SETKEYGB, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_FullSync = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_RDPFULLSYNC;
			dpDMA.writeDMA(Gbi.G_RDPFULLSYNC, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_TileSync = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_RDPTILESYNC;
			dpDMA.writeDMA(Gbi.G_RDPTILESYNC, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_PipeSync = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_RDPPIPESYNC;
			dpDMA.writeDMA(Gbi.G_RDPPIPESYNC, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_LoadSync = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			cmd[0] = Gbi.G_RDPLOADSYNC;
			dpDMA.writeDMA(Gbi.G_RDPLOADSYNC, cmdbuf, 0, 8);
		}
	};

	protected OpCode RDP_TexRectFlip = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			getWord();
			int w3 = getWord();
			getWord();
			int w4 = getWord();

			updateStates();

			cmd[0] = Gbi.G_TEXRECTFLIP;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			cmd[8] = (byte) (w3 >> 24);
			cmd[9] = (byte) (w3 >> 16);
			cmd[10] = (byte) (w3 >> 8);
			cmd[11] = (byte) (w3 >> 0);
			cmd[12] = (byte) (w4 >> 24);
			cmd[13] = (byte) (w4 >> 16);
			cmd[14] = (byte) (w4 >> 8);
			cmd[15] = (byte) (w4 >> 0);
			dpDMA.writeDMA(Gbi.G_TEXRECTFLIP, cmdbuf, 0, 16);
		}
	};

	protected OpCode RDP_TexRect = new OpCode()
	{
		public void exec(int w1, int w2)
		{
			getWord();
			int w3 = getWord();
			getWord();
			int w4 = getWord();

			updateStates();

			cmd[0] = Gbi.G_TEXRECT;
			cmd[1] = (byte) (w1 >> 16);
			cmd[2] = (byte) (w1 >> 8);
			cmd[3] = (byte) (w1 >> 0);
			cmd[4] = (byte) (w2 >> 24);
			cmd[5] = (byte) (w2 >> 16);
			cmd[6] = (byte) (w2 >> 8);
			cmd[7] = (byte) (w2 >> 0);
			cmd[8] = (byte) (w3 >> 24);
			cmd[9] = (byte) (w3 >> 16);
			cmd[10] = (byte) (w3 >> 8);
			cmd[11] = (byte) (w3 >> 0);
			cmd[12] = (byte) (w4 >> 24);
			cmd[13] = (byte) (w4 >> 16);
			cmd[14] = (byte) (w4 >> 8);
			cmd[15] = (byte) (w4 >> 0);
			dpDMA.writeDMA(Gbi.G_TEXRECT, cmdbuf, 0, 16);
		}
	};
}
