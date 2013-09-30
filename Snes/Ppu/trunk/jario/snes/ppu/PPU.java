package jario.snes.ppu;

import jario.hardware.Bus1bit;
import jario.hardware.Bus8bit;
import jario.hardware.BusDMA;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class PPU implements Hardware, Clockable, Bus1bit, Bus8bit, BusDMA, Configurable
{
	public static final int NTSC = 0;
	public static final int PAL = 1;
	
	static class Output
	{
		static class Pixel
		{
			public int priority; // 0 = none (transparent)
			public int palette;
			public int tile;
		}

		public Pixel main = new Pixel();
		public Pixel sub = new Pixel();

		public int id;

		public Output(int id)
		{
			this.id = id;
		}
	}

	private int region;

	int[] vram;
	int[] oamram;
	int[] cgram;

	private long clock;
	int ticks;
	private int pc = 1;
	private int address = 1;
	private int pixel = 256;
	private boolean accuracy;
	private boolean requestAccuracy;

	@Override
	public void connect(int port, Hardware hw)
	{
	}

	final void latch_counters()
	{
		// CPU.cpu.synchronize_ppu();
		regs.hcounter = counter.hdot() & 0xFFFF;
		regs.vcounter = counter.vcounter() & 0xFFFF;
		regs.counters_latched = true;
	}
	
	@Override
	public boolean read1bit(int address)
	{
		switch (address)
		{
		case 0: return display.interlace;
		case 1: return display.overscan;
		case 2: return display.hires;
		case 3: return counter.field();
		default: return false;
		}
	}
	
	@Override
	public void write1bit(int address, boolean data)
	{
		switch (address)
		{
		case 29:
			if (display.latch != 0 && data == false)
			{
				latch_counters();
			}
			display.latch = data ? 1 : 0;
			break;
		}
	}

	@Override
	public final void clock(long clocks)
	{
		clock -= clocks;
		while (clock < 0L)
		{
			if (accuracy)
			{
				switch (pc)
				{
				case 0:
					counter.tick(2);
					clock += 2L;
					// synchronize_cpu();

					if (--ticks == 0)
					{
						pc = address;
						if (pc == 1)
						{
							accuracy = requestAccuracy;
						}
					}
					break;
				case 1:
					scanline();
					// add_clocks(60);
					ticks = 14; // 28 >> 1;
					address = 2;
					pc = 0;
					break;
				case 2:
					if (counter.vcounter() <= (!regs.overscan ? 224 : 239))
					{
						if (counter.vcounter() != 0)
						{
							bg1.run(true);
							bg2.run(true);
							bg3.run(true);
							bg4.run(true);
						}
						pixel = -7;
						// add_clocks(2);
						ticks = 1; // 2 >> 1;
						address = 7;
					}
					else
					{
						// add_clocks(1210); // 1052 + 22 + 136
						ticks = 601; // 1202 >> 1;
						address = 5;
					}
					pc = 0;
					break;
				case 3:
					// add_clocks(22);
					ticks = 7; // 14 >> 1;
					address = 4;
					pc = 0;
					break;
				case 4:
					if (sprite.tilefetch_cycle())
					{
						if (ticks > 0)
						{
							address = 5;
						}
						else
						{
							ticks = (counter.lineclocks() - 1230) >> 1;
							address = 1;
						}
					}
					pc = 0;
					break;
				case 5:
					// add_clocks(counter.lineclocks() - 1270); // 60 - 1052 - 22 - 136
					ticks = (counter.lineclocks() - 1230) >> 1;
					address = 1;
					pc = 0;
					break;
				case 6:
					if (counter.vcounter() != 0)
					{
						bg1.run(true);
						bg2.run(true);
						bg3.run(true);
						bg4.run(true);
					}
					// add_clocks(2);
					ticks = 1; // 2 >> 1;
					address = 7;
					pc = 0;
					break;
				case 7:
					if (counter.vcounter() != 0)
					{
						bg1.run(false);
						bg2.run(false);
						bg3.run(false);
						bg4.run(false);
					}
					if (pixel >= 0)
					{
						sprite.run();
						window.run();
						screen.run();
					}
					// add_clocks(2);
					ticks = 1; // 2 >> 1;
					if (++pixel <= 255)
					{
						address = 6;
					}
					else
					{
						address = 3;
					}
					pc = 0;
					break;
				}
			}
			else
			{
				scanline();
				add_clocks(60);

				if (counter.vcounter() <= (!regs.overscan ? 224 : 239))
				{
					for (int pixel = -7; pixel <= 255; pixel++)
					{
						if (counter.vcounter() != 0)
						{
							bg1.run(true);
							bg2.run(true);
							bg3.run(true);
							bg4.run(true);
						}
						add_clocks(2);

						if (counter.vcounter() != 0)
						{
							bg1.run(false);
							bg2.run(false);
							bg3.run(false);
							bg4.run(false);
						}
						if (pixel >= 0)
						{
							sprite.run();
							window.run();
							screen.run();
						}
						add_clocks(2);
					}

					add_clocks(22);
					sprite.tilefetch();
				}
				else
				{
					add_clocks(1210); // 1052 + 22 + 136
				}

				add_clocks(counter.lineclocks() - 1270); // 60 - 1052 - 22 - 136
				accuracy = requestAccuracy;
			}
		}
	}

	private void power()
	{
		Arrays.fill(vram, 0);
		Arrays.fill(oamram, 0);
		Arrays.fill(cgram, 0);
		reset();
	}

	@Override
	public void reset()
	{
		clock = 0;
		pc = 1;

		if (counter != null)
		{
			counter.reset();
		}
		Arrays.fill(output.array(), (byte) 0);

		mmio_reset();
		bg1.reset();
		bg2.reset();
		bg3.reset();
		bg4.reset();
		sprite.reset();
		window.reset();
		screen.reset();

		if (display != null)
		{
			display.latch = 1;
			display.hires = true;

			frame();
		}
	}

	public PPU()
	{
		vram = new int[64 * 1024];
		oamram = new int[544];
		cgram = new int[512];
		bg1 = new Background(this, Background.ID_BG1);
		bg2 = new Background(this, Background.ID_BG2);
		bg3 = new Background(this, Background.ID_BG3);
		bg4 = new Background(this, Background.ID_BG4);
		sprite = new Sprite(this);
		window = new Window(this);
		screen = new Screen(this);
		output = ByteBuffer.allocate(1024 * 1024 * 2);
		
		display = new Display();
		display.latch = 1;
		display.hires = true;
		
		counter = new HVCounter(this);

		power();
	}

	Regs regs = new Regs();

	private int get_vram_address()
	{
		int addr = regs.vram_addr;
		switch (regs.vram_mapping)
		{
		case 0:
			break; // direct mapping
		case 1:
			addr = (addr & 0xff00) | ((addr & 0x001f) << 3) | ((addr >> 5) & 7);
			break;
		case 2:
			addr = (addr & 0xfe00) | ((addr & 0x003f) << 3) | ((addr >> 6) & 7);
			break;
		case 3:
			addr = (addr & 0xfc00) | ((addr & 0x007f) << 3) | ((addr >> 7) & 7);
			break;
		}
		return (addr << 1) & 0xFFFF;
	}

	private int vram_read(int addr)
	{
		if (regs.display_disable || counter.vcounter() >= (!regs.overscan ? 225 : 240)) { return vram[addr]; }
		return 0x00;
	}

	private void vram_write(int addr, int data)
	{
		if (regs.display_disable || counter.vcounter() >= (!regs.overscan ? 225 : 240))
		{
			vram[addr] = data & 0xFF;
		}
	}

	private void mmio_update_video_mode()
	{
		switch (regs.bgmode)
		{
		case 0:
		{
			bg1.mode = Background.Mode_BPP2;
			bg1.priority0 = 8;
			bg1.priority1 = 11;
			bg2.mode = Background.Mode_BPP2;
			bg2.priority0 = 7;
			bg2.priority1 = 10;
			bg3.mode = Background.Mode_BPP2;
			bg3.priority0 = 2;
			bg3.priority1 = 5;
			bg4.mode = Background.Mode_BPP2;
			bg4.priority0 = 1;
			bg4.priority1 = 4;
			sprite.priority0 = 3;
			sprite.priority1 = 6;
			sprite.priority2 = 9;
			sprite.priority3 = 12;
		}
			break;
		case 1:
		{
			bg1.mode = Background.Mode_BPP4;
			bg2.mode = Background.Mode_BPP4;
			bg3.mode = Background.Mode_BPP2;
			bg4.mode = Background.Mode_Inactive;
			if (regs.bg3_priority)
			{
				bg1.priority0 = 5;
				bg1.priority1 = 8;
				bg2.priority0 = 4;
				bg2.priority1 = 7;
				bg3.priority0 = 1;
				bg3.priority1 = 10;
				sprite.priority0 = 2;
				sprite.priority1 = 3;
				sprite.priority2 = 6;
				sprite.priority3 = 9;
			}
			else
			{
				bg1.priority0 = 6;
				bg1.priority1 = 9;
				bg2.priority0 = 5;
				bg2.priority1 = 8;
				bg3.priority0 = 1;
				bg3.priority1 = 3;
				sprite.priority0 = 2;
				sprite.priority1 = 4;
				sprite.priority2 = 7;
				sprite.priority3 = 10;
			}
		}
			break;
		case 2:
		{
			bg1.mode = Background.Mode_BPP4;
			bg2.mode = Background.Mode_BPP4;
			bg3.mode = Background.Mode_Inactive;
			bg4.mode = Background.Mode_Inactive;
			bg1.priority0 = 3;
			bg1.priority1 = 7;
			bg2.priority0 = 1;
			bg2.priority1 = 5;
			sprite.priority0 = 2;
			sprite.priority1 = 4;
			sprite.priority2 = 6;
			sprite.priority3 = 8;
		}
			break;
		case 3:
		{
			bg1.mode = Background.Mode_BPP8;
			bg2.mode = Background.Mode_BPP4;
			bg3.mode = Background.Mode_Inactive;
			bg4.mode = Background.Mode_Inactive;
			bg1.priority0 = 3;
			bg1.priority1 = 7;
			bg2.priority0 = 1;
			bg2.priority1 = 5;
			sprite.priority0 = 2;
			sprite.priority1 = 4;
			sprite.priority2 = 6;
			sprite.priority3 = 8;
		}
			break;
		case 4:
		{
			bg1.mode = Background.Mode_BPP8;
			bg2.mode = Background.Mode_BPP2;
			bg3.mode = Background.Mode_Inactive;
			bg4.mode = Background.Mode_Inactive;
			bg1.priority0 = 3;
			bg1.priority1 = 7;
			bg2.priority0 = 1;
			bg2.priority1 = 5;
			sprite.priority0 = 2;
			sprite.priority1 = 4;
			sprite.priority2 = 6;
			sprite.priority3 = 8;
		}
			break;
		case 5:
		{
			bg1.mode = Background.Mode_BPP4;
			bg2.mode = Background.Mode_BPP2;
			bg3.mode = Background.Mode_Inactive;
			bg4.mode = Background.Mode_Inactive;
			bg1.priority0 = 3;
			bg1.priority1 = 7;
			bg2.priority0 = 1;
			bg2.priority1 = 5;
			sprite.priority0 = 2;
			sprite.priority1 = 4;
			sprite.priority2 = 6;
			sprite.priority3 = 8;
		}
			break;
		case 6:
		{
			bg1.mode = Background.Mode_BPP4;
			bg2.mode = Background.Mode_Inactive;
			bg3.mode = Background.Mode_Inactive;
			bg4.mode = Background.Mode_Inactive;
			bg1.priority0 = 2;
			bg1.priority1 = 5;
			sprite.priority0 = 1;
			sprite.priority1 = 3;
			sprite.priority2 = 4;
			sprite.priority3 = 6;
		}
			break;
		case 7:
		{
			if (regs.mode7_extbg == false)
			{
				bg1.mode = Background.Mode_Mode7;
				bg2.mode = Background.Mode_Inactive;
				bg3.mode = Background.Mode_Inactive;
				bg4.mode = Background.Mode_Inactive;
				bg1.priority0 = 2;
				bg1.priority1 = 2;
				sprite.priority0 = 1;
				sprite.priority1 = 3;
				sprite.priority2 = 4;
				sprite.priority3 = 5;
			}
			else
			{
				bg1.mode = Background.Mode_Mode7;
				bg2.mode = Background.Mode_Mode7;
				bg3.mode = Background.Mode_Inactive;
				bg4.mode = Background.Mode_Inactive;
				bg1.priority0 = 3;
				bg1.priority1 = 3;
				bg2.priority0 = 1;
				bg2.priority1 = 5;
				sprite.priority0 = 2;
				sprite.priority1 = 4;
				sprite.priority2 = 6;
				sprite.priority3 = 7;
			}
		}
			break;
		}
	}

	private void mmio_w2100(int data)
	{
		if (regs.display_disable && counter.vcounter() == (!regs.overscan ? 225 : 240))
		{
			sprite.address_reset();
		}
		regs.display_disable = (data & 0x80) != 0;
		regs.display_brightness = data & 0x0f;
	} // INIDISP

	private void mmio_w2101(int data)
	{
		sprite.base_size = (data >> 5) & 7;
		sprite.nameselect = (data >> 3) & 3;
		sprite.tiledata_addr = (data & 3) << 14;
	} // OBSEL

	private void mmio_w2102(int data)
	{
		regs.oam_baseaddr = (regs.oam_baseaddr & 0x0200) | ((data & 0xFF) << 1);
		sprite.address_reset();
	} // OAMADDL

	private void mmio_w2103(int data)
	{
		regs.oam_priority = (data & 0x80) != 0;
		regs.oam_baseaddr = ((data & 0x01) << 9) | (regs.oam_baseaddr & 0x01fe);
		sprite.address_reset();
	} // OAMADDH

	private void mmio_w2104(int data)
	{
		boolean latch = (regs.oam_addr & 1) != 0;
		int addr = regs.oam_addr;
		regs.oam_addr = (regs.oam_addr + 1) & 0x3FF;
		if (regs.display_disable == false && counter.vcounter() < (!regs.overscan ? 225 : 240))
		{
			addr = regs.oam_iaddr & 0x3FF;
		}
		if ((addr & 0x0200) != 0)
		{
			addr &= 0x021f;
		}

		if (latch == false)
		{
			regs.oam_latchdata = data & 0xFF;
		}
		if ((addr & 0x0200) != 0)
		{
			sprite.update(addr, data);
		}
		else if (latch == true)
		{
			sprite.update((addr & ~1) + 0, regs.oam_latchdata);
			sprite.update((addr & ~1) + 1, data);
		}
		sprite.set_first_sprite();
	} // OAMDATA

	private void mmio_w2105(int data)
	{
		bg4.tile_size = (data & 0x80) != 0;
		bg3.tile_size = (data & 0x40) != 0;
		bg2.tile_size = (data & 0x20) != 0;
		bg1.tile_size = (data & 0x10) != 0;
		regs.bg3_priority = (data & 0x08) != 0;
		regs.bgmode = data & 0x07;
		mmio_update_video_mode();
	} // BGMODE

	private void mmio_w2106(int data)
	{
		int mosaic_size = (data >> 4) & 15;
		bg4.mosaic = ((data & 0x08) != 0 ? mosaic_size : 0);
		bg3.mosaic = ((data & 0x04) != 0 ? mosaic_size : 0);
		bg2.mosaic = ((data & 0x02) != 0 ? mosaic_size : 0);
		bg1.mosaic = ((data & 0x01) != 0 ? mosaic_size : 0);
	} // MOSAIC

	private void mmio_w2107(int data)
	{
		bg1.screen_addr = (data & 0x7c) << 9;
		bg1.screen_size = (data & 3);
	} // BG1SC

	private void mmio_w2108(int data)
	{
		bg2.screen_addr = (data & 0x7c) << 9;
		bg2.screen_size = (data & 3);
	} // BG2SC

	private void mmio_w2109(int data)
	{
		bg3.screen_addr = (data & 0x7c) << 9;
		bg3.screen_size = (data & 3);
	} // BG3SC

	private void mmio_w210a(int data)
	{
		bg4.screen_addr = (data & 0x7c) << 9;
		bg4.screen_size = (data & 3);
	} // BG4SC

	private void mmio_w210b(int data)
	{
		bg1.tiledata_addr = (data & 0x07) << 13;
		bg2.tiledata_addr = (data & 0x70) << 9;
	} // BG12NBA

	private void mmio_w210c(int data)
	{
		bg3.tiledata_addr = (data & 0x07) << 13;
		bg4.tiledata_addr = (data & 0x70) << 9;
	} // BG34NBA

	private void mmio_w210d(int data)
	{
		regs.mode7_hoffset = (((data & 0xFF) << 8) | regs.mode7_latchdata) & 0xFFFF;
		regs.mode7_latchdata = data & 0xFF;

		bg1.hoffset = ((data & 0xFF) << 8) | (regs.bgofs_latchdata & ~7) | ((bg1.hoffset >> 8) & 7);
		regs.bgofs_latchdata = data & 0xFF;
	} // BG1HOFS

	private void mmio_w210e(int data)
	{
		regs.mode7_voffset = (((data & 0xFF) << 8) | regs.mode7_latchdata) & 0xFFFF;
		regs.mode7_latchdata = data & 0xFF;

		bg1.voffset = ((data & 0xFF) << 8) | regs.bgofs_latchdata;
		regs.bgofs_latchdata = data & 0xFF;
	} // BG1VOFS

	private void mmio_w210f(int data)
	{
		bg2.hoffset = ((data & 0xFF) << 8) | (regs.bgofs_latchdata & ~7) | ((bg2.hoffset >> 8) & 7);
		regs.bgofs_latchdata = data & 0xFF;
	} // BG2HOFS

	private void mmio_w2110(int data)
	{
		bg2.voffset = ((data & 0xFF) << 8) | regs.bgofs_latchdata;
		regs.bgofs_latchdata = data & 0xFF;
	} // BG2VOFS

	private void mmio_w2111(int data)
	{
		bg3.hoffset = ((data & 0xFF) << 8) | (regs.bgofs_latchdata & ~7) | ((bg3.hoffset >> 8) & 7);
		regs.bgofs_latchdata = data & 0xFF;
	} // BG3HOFS

	private void mmio_w2112(int data)
	{
		bg3.voffset = ((data & 0xFF) << 8) | regs.bgofs_latchdata;
		regs.bgofs_latchdata = data & 0xFF;
	} // BG3VOFS

	private void mmio_w2113(int data)
	{
		bg4.hoffset = ((data & 0xFF) << 8) | (regs.bgofs_latchdata & ~7) | ((bg4.hoffset >> 8) & 7);
		regs.bgofs_latchdata = data & 0xFF;
	} // BG4HOFS

	private void mmio_w2114(int data)
	{
		bg4.voffset = ((data & 0xFF) << 8) | regs.bgofs_latchdata;
		regs.bgofs_latchdata = data & 0xFF;
	} // BG4VOFS

	private void mmio_w2115(int data)
	{
		regs.vram_incmode = (data & 0x80) != 0;
		regs.vram_mapping = (data >> 2) & 3;
		switch (data & 3)
		{
		case 0:
			regs.vram_incsize = 1;
			break;
		case 1:
			regs.vram_incsize = 32;
			break;
		case 2:
			regs.vram_incsize = 128;
			break;
		case 3:
			regs.vram_incsize = 128;
			break;
		}
	} // VMAIN

	private void mmio_w2116(int data)
	{
		regs.vram_addr &= 0xff00;
		regs.vram_addr |= ((data & 0xFF) << 0);
		int addr = get_vram_address();
		regs.vram_readbuffer = vram_read(addr + 0) << 0;
		regs.vram_readbuffer |= (vram_read(addr + 1) << 8);
	} // VMADDL

	private void mmio_w2117(int data)
	{
		regs.vram_addr &= 0x00ff;
		regs.vram_addr |= ((data & 0xFF) << 8);
		int addr = get_vram_address();
		regs.vram_readbuffer = vram_read(addr + 0) << 0;
		regs.vram_readbuffer |= (vram_read(addr + 1) << 8);
	} // VMADDH

	private void mmio_w2118(int data)
	{
		int addr = get_vram_address() + 0;
		vram_write(addr, data);
		if (regs.vram_incmode == false)
		{
			regs.vram_addr += regs.vram_incsize;
		}
	} // VMDATAL

	private void mmio_w2119(int data)
	{
		int addr = get_vram_address() + 1;
		vram_write(addr, data);
		if (regs.vram_incmode == true)
		{
			regs.vram_addr += regs.vram_incsize;
		}
	} // VMDATAH

	private void mmio_w211a(int data)
	{
		regs.mode7_repeat = (data >> 6) & 3;
		regs.mode7_vflip = (data & 0x02) != 0;
		regs.mode7_hflip = (data & 0x01) != 0;
	} // M7SEL

	private void mmio_w211b(int data)
	{
		regs.m7a = (((data & 0xFF) << 8) | regs.mode7_latchdata) & 0xFFFF;
		regs.mode7_latchdata = data & 0xFF;
	} // M7A

	private void mmio_w211c(int data)
	{
		regs.m7b = (((data & 0xFF) << 8) | regs.mode7_latchdata) & 0xFFFF;
		regs.mode7_latchdata = data & 0xFF;
	} // M7B

	private void mmio_w211d(int data)
	{
		regs.m7c = (((data & 0xFF) << 8) | regs.mode7_latchdata) & 0xFFFF;
		regs.mode7_latchdata = data & 0xFF;
	} // M7C

	private void mmio_w211e(int data)
	{
		regs.m7d = (((data & 0xFF) << 8) | regs.mode7_latchdata) & 0xFFFF;
		regs.mode7_latchdata = data & 0xFF;
	} // M7D

	private void mmio_w211f(int data)
	{
		regs.m7x = (((data & 0xFF) << 8) | regs.mode7_latchdata) & 0xFFFF;
		regs.mode7_latchdata = data & 0xFF;
	} // M7X

	private void mmio_w2120(int data)
	{
		regs.m7y = (((data & 0xFF) << 8) | regs.mode7_latchdata) & 0xFFFF;
		regs.mode7_latchdata = data & 0xFF;
	} // M7Y

	private void mmio_w2121(int data)
	{
		regs.cgram_addr = (data & 0xFF) << 1;
	} // CGADD

	private void mmio_w2122(int data)
	{
		boolean latch = (regs.cgram_addr & 1) != 0;
		int addr = regs.cgram_addr;
		regs.cgram_addr = (regs.cgram_addr + 1) & 0x1FF;
		if (regs.display_disable == false && counter.vcounter() > 0 && counter.vcounter() < (!regs.overscan ? 225 : 240) && counter.hcounter() >= 88 && counter.hcounter() < 1096)
		{
			addr = regs.cgram_iaddr & 0x1FF;
		}

		if (latch == false)
		{
			regs.cgram_latchdata = data & 0xFF;
		}
		else
		{
			cgram[(addr & ~1) + 0] = regs.cgram_latchdata & 0xFF;
			cgram[(addr & ~1) + 1] = (data & 0x7f);
		}
	} // CGDATA

	private void mmio_w2123(int data)
	{
		window.bg2_two_enable = (data & 0x80) != 0;
		window.bg2_two_invert = (data & 0x40) != 0;
		window.bg2_one_enable = (data & 0x20) != 0;
		window.bg2_one_invert = (data & 0x10) != 0;
		window.bg1_two_enable = (data & 0x08) != 0;
		window.bg1_two_invert = (data & 0x04) != 0;
		window.bg1_one_enable = (data & 0x02) != 0;
		window.bg1_one_invert = (data & 0x01) != 0;
	} // W12SEL

	private void mmio_w2124(int data)
	{
		window.bg4_two_enable = (data & 0x80) != 0;
		window.bg4_two_invert = (data & 0x40) != 0;
		window.bg4_one_enable = (data & 0x20) != 0;
		window.bg4_one_invert = (data & 0x10) != 0;
		window.bg3_two_enable = (data & 0x08) != 0;
		window.bg3_two_invert = (data & 0x04) != 0;
		window.bg3_one_enable = (data & 0x02) != 0;
		window.bg3_one_invert = (data & 0x01) != 0;
	} // W34SEL

	private void mmio_w2125(int data)
	{
		window.col_two_enable = (data & 0x80) != 0;
		window.col_two_invert = (data & 0x40) != 0;
		window.col_one_enable = (data & 0x20) != 0;
		window.col_one_invert = (data & 0x10) != 0;
		window.oam_two_enable = (data & 0x08) != 0;
		window.oam_two_invert = (data & 0x04) != 0;
		window.oam_one_enable = (data & 0x02) != 0;
		window.oam_one_invert = (data & 0x01) != 0;
	} // WOBJSEL

	private void mmio_w2126(int data)
	{
		window.one_left = data & 0xFF;
	} // WH0

	private void mmio_w2127(int data)
	{
		window.one_right = data & 0xFF;
	} // WH1

	private void mmio_w2128(int data)
	{
		window.two_left = data & 0xFF;
	} // WH2

	private void mmio_w2129(int data)
	{
		window.two_right = data & 0xFF;
	} // WH3

	private void mmio_w212a(int data)
	{
		window.bg4_mask = (data >> 6) & 3;
		window.bg3_mask = (data >> 4) & 3;
		window.bg2_mask = (data >> 2) & 3;
		window.bg1_mask = (data >> 0) & 3;
	} // WBGLOG

	private void mmio_w212b(int data)
	{
		window.col_mask = (data >> 2) & 3;
		window.oam_mask = (data >> 0) & 3;
	} // WOBJLOG

	private void mmio_w212c(int data)
	{
		sprite.main_enable = (data & 0x10) != 0;
		bg4.main_enable = (data & 0x08) != 0;
		bg3.main_enable = (data & 0x04) != 0;
		bg2.main_enable = (data & 0x02) != 0;
		bg1.main_enable = (data & 0x01) != 0;
	} // TM

	private void mmio_w212d(int data)
	{
		sprite.sub_enable = (data & 0x10) != 0;
		bg4.sub_enable = (data & 0x08) != 0;
		bg3.sub_enable = (data & 0x04) != 0;
		bg2.sub_enable = (data & 0x02) != 0;
		bg1.sub_enable = (data & 0x01) != 0;
	} // TS

	private void mmio_w212e(int data)
	{
		window.oam_main_enable = (data & 0x10) != 0;
		window.bg4_main_enable = (data & 0x08) != 0;
		window.bg3_main_enable = (data & 0x04) != 0;
		window.bg2_main_enable = (data & 0x02) != 0;
		window.bg1_main_enable = (data & 0x01) != 0;
	} // TMW

	private void mmio_w212f(int data)
	{
		window.oam_sub_enable = (data & 0x10) != 0;
		window.bg4_sub_enable = (data & 0x08) != 0;
		window.bg3_sub_enable = (data & 0x04) != 0;
		window.bg2_sub_enable = (data & 0x02) != 0;
		window.bg1_sub_enable = (data & 0x01) != 0;
	} // TSW

	private void mmio_w2130(int data)
	{
		window.col_main_mask = (data >> 6) & 3;
		window.col_sub_mask = (data >> 4) & 3;
		screen.addsub_mode = (data & 0x02) != 0;
		screen.direct_color = (data & 0x01) != 0;
	} // CGWSEL

	private void mmio_w2131(int data)
	{
		screen.color_mode = (data & 0x80) != 0;
		screen.color_halve = (data & 0x40) != 0;
		screen.back_color_enable = (data & 0x20) != 0;
		screen.oam_color_enable = (data & 0x10) != 0;
		screen.bg4_color_enable = (data & 0x08) != 0;
		screen.bg3_color_enable = (data & 0x04) != 0;
		screen.bg2_color_enable = (data & 0x02) != 0;
		screen.bg1_color_enable = (data & 0x01) != 0;
	} // CGADDSUB

	private void mmio_w2132(int data)
	{
		if ((data & 0x80) != 0)
		{
			screen.color_b = data & 0x1f;
		}
		if ((data & 0x40) != 0)
		{
			screen.color_g = data & 0x1f;
		}
		if ((data & 0x20) != 0)
		{
			screen.color_r = data & 0x1f;
		}
	} // COLDATA

	private void mmio_w2133(int data)
	{
		regs.mode7_extbg = (data & 0x40) != 0;
		regs.pseudo_hires = (data & 0x08) != 0;
		regs.overscan = (data & 0x04) != 0;
		sprite.interlace = (data & 0x02) != 0;
		regs.interlace = (data & 0x01) != 0;
		mmio_update_video_mode();
	} // SETINI

	private int mmio_r2134()
	{
		int result = (short) regs.m7a * (byte) (regs.m7b >> 8);
		regs.ppu1_mdr = (result >> 0) & 0xFF;
		return regs.ppu1_mdr;
	} // MPYL

	private int mmio_r2135()
	{
		int result = (short) regs.m7a * (byte) (regs.m7b >> 8);
		regs.ppu1_mdr = (result >> 8) & 0xFF;
		return regs.ppu1_mdr;
	} // MPYM

	private int mmio_r2136()
	{
		int result = (short) regs.m7a * (byte) (regs.m7b >> 8);
		regs.ppu1_mdr = (result >> 16) & 0xFF;
		return regs.ppu1_mdr;
	} // MPYH

	private int mmio_r2137()
	{
		if (display.latch != 0)
		{
			latch_counters();
		}
		return regs.ppu1_mdr; // CPU.cpu.regs.mdr & 0xFF;
	} // SLHV

	private int mmio_r2138()
	{
		int addr = regs.oam_addr;
		regs.oam_addr = (regs.oam_addr + 1) & 0x3FF;
		if (regs.display_disable == false && counter.vcounter() < (!regs.overscan ? 225 : 240))
		{
			addr = regs.oam_iaddr & 0x3FF;
		}
		if ((addr & 0x0200) != 0)
		{
			addr &= 0x021f;
		}

		regs.ppu1_mdr = oamram[addr];
		sprite.set_first_sprite();
		return regs.ppu1_mdr;
	} // OAMDATAREAD

	private int mmio_r2139()
	{
		int addr = get_vram_address() + 0;
		regs.ppu1_mdr = (regs.vram_readbuffer >> 0) & 0xFF;
		if (regs.vram_incmode == false)
		{
			addr &= ((~1) & 0xFFFF);
			regs.vram_readbuffer = (vram_read(addr + 0) << 0);
			regs.vram_readbuffer |= (vram_read(addr + 1) << 8);
			regs.vram_addr += regs.vram_incsize;
		}
		return regs.ppu1_mdr;
	} // VMDATALREAD

	private int mmio_r213a()
	{
		int addr = get_vram_address() + 1;
		regs.ppu1_mdr = (regs.vram_readbuffer >> 8) & 0xFF;
		if (regs.vram_incmode == true)
		{
			addr &= ((~1) & 0xFFFF);
			regs.vram_readbuffer = (vram_read(addr + 0) << 0);
			regs.vram_readbuffer |= (vram_read(addr + 1) << 8);
			regs.vram_addr += regs.vram_incsize;
		}
		return regs.ppu1_mdr;
	} // VMDATAHREAD

	private int mmio_r213b()
	{
		boolean latch = (regs.cgram_addr & 1) != 0;
		int addr = regs.cgram_addr++;
		regs.cgram_addr &= 0x1FF;
		if (regs.display_disable == false && counter.vcounter() > 0 && counter.vcounter() < (!regs.overscan ? 225 : 240) && counter.hcounter() >= 88 && counter.hcounter() < 1096)
		{
			addr = regs.cgram_iaddr;
		}

		if (latch == false)
		{
			regs.ppu2_mdr = cgram[addr];
		}
		else
		{
			regs.ppu2_mdr &= 0x80;
			regs.ppu2_mdr |= cgram[addr];
		}
		return regs.ppu2_mdr;
	} // CGDATAREAD

	private int mmio_r213c()
	{
		if (regs.latch_hcounter == false)
		{
			regs.ppu2_mdr = (regs.hcounter >> 0) & 0xFF;
		}
		else
		{
			regs.ppu2_mdr &= 0xfe;
			regs.ppu2_mdr |= ((regs.hcounter >> 8) & 1);
		}
		regs.latch_hcounter = ((regs.latch_hcounter ? 1 : 0) ^ 1) != 0;
		return regs.ppu2_mdr;
	} // OPHCT

	private int mmio_r213d()
	{
		if (regs.latch_vcounter == false)
		{
			regs.ppu2_mdr = (regs.vcounter >> 0) & 0xFF;
		}
		else
		{
			regs.ppu2_mdr &= 0xfe;
			regs.ppu2_mdr |= ((regs.vcounter >> 8) & 1);
		}
		regs.latch_vcounter = ((regs.latch_vcounter ? 1 : 0) ^ 1) != 0;
		return regs.ppu2_mdr;
	} // OPVCT

	private int mmio_r213e()
	{
		regs.ppu1_mdr &= 0x10;
		regs.ppu1_mdr |= ((sprite.time_over ? 1 : 0) << 7);
		regs.ppu1_mdr |= ((sprite.range_over ? 1 : 0) << 6);
		regs.ppu1_mdr |= (ppu1_version & 0x0f);
		return regs.ppu1_mdr;
	} // STAT77

	private int mmio_r213f()
	{
		regs.latch_hcounter = false;
		regs.latch_vcounter = false;

		regs.ppu2_mdr &= 0x20;
		regs.ppu2_mdr |= ((counter.field() ? 1 : 0) << 7);
		if (display.latch == 0)
		{
			regs.ppu2_mdr |= 0x40;
		}
		else if (regs.counters_latched)
		{
			regs.ppu2_mdr |= 0x40;
			regs.counters_latched = false;
		}
		regs.ppu2_mdr |= ((region == NTSC ? 0 : 1) << 4);
		regs.ppu2_mdr |= (ppu2_version & 0x0f);
		return regs.ppu2_mdr;
	} // STAT78

	private void mmio_reset()
	{
		regs.ppu1_mdr = 0xff;
		regs.ppu2_mdr = 0xff;

		regs.vram_readbuffer = 0x0000;
		regs.oam_latchdata = 0x00;
		regs.cgram_latchdata = 0x00;
		regs.bgofs_latchdata = 0x00;
		regs.mode7_latchdata = 0x00;
		regs.counters_latched = false;
		regs.latch_hcounter = false;
		regs.latch_vcounter = false;

		regs.oam_iaddr = 0x0000;
		regs.cgram_iaddr = 0x00;

		// $2100 INIDISP
		regs.display_disable = true;
		regs.display_brightness = 0;

		// $2102 OAMADDL
		// $2103 OAMADDH
		regs.oam_baseaddr = 0x0000;
		regs.oam_addr = 0x0000;
		regs.oam_priority = false;

		// $2105 BGMODE
		regs.bg3_priority = false;
		regs.bgmode = 0;

		// $210d BG1HOFS
		regs.mode7_hoffset = 0x0000;

		// $210e BG1VOFS
		regs.mode7_voffset = 0x0000;

		// $2115 VMAIN
		regs.vram_incmode = true;
		regs.vram_mapping = 0;
		regs.vram_incsize = 1;

		// $2116 VMADDL
		// $2117 VMADDH
		regs.vram_addr = 0x0000;

		// $211a M7SEL
		regs.mode7_repeat = 0;
		regs.mode7_vflip = false;
		regs.mode7_hflip = false;

		// $211b M7A
		regs.m7a = 0x0000;

		// $211c M7B
		regs.m7b = 0x0000;

		// $211d M7C
		regs.m7c = 0x0000;

		// $211e M7D
		regs.m7d = 0x0000;

		// $211f M7X
		regs.m7x = 0x0000;

		// $2120 M7Y
		regs.m7y = 0x0000;

		// $2121 CGADD
		regs.cgram_addr = 0x0000;

		// $2133 SETINI
		regs.mode7_extbg = false;
		regs.pseudo_hires = false;
		regs.overscan = false;
		regs.interlace = false;

		// $213c OPHCT
		regs.hcounter = 0;

		// $213d OPVCT
		regs.vcounter = 0;
	}

	@Override
	public byte read8bit(int addr)
	{
		// CPU.cpu.synchronize_ppu();

		switch (addr & 0xffff)
		{
		case 0x2134:
			return (byte) mmio_r2134(); // MPYL
		case 0x2135:
			return (byte) mmio_r2135(); // MPYM
		case 0x2136:
			return (byte) mmio_r2136(); // MYPH
		case 0x2137:
			return (byte) mmio_r2137(); // SLHV
		case 0x2138:
			return (byte) mmio_r2138(); // OAMDATAREAD
		case 0x2139:
			return (byte) mmio_r2139(); // VMDATALREAD
		case 0x213a:
			return (byte) mmio_r213a(); // VMDATAHREAD
		case 0x213b:
			return (byte) mmio_r213b(); // CGDATAREAD
		case 0x213c:
			return (byte) mmio_r213c(); // OPHCT
		case 0x213d:
			return (byte) mmio_r213d(); // OPVCT
		case 0x213e:
			return (byte) mmio_r213e(); // STAT77
		case 0x213f:
			return (byte) mmio_r213f(); // STAT78
		}

		return (byte) regs.ppu1_mdr;
	}

	@Override
	public void write8bit(int addr, byte data)
	{
		// CPU.cpu.synchronize_ppu();

		switch (addr & 0xffff)
		{
		case 0x2100:
			mmio_w2100(data); // INIDISP
			return;
		case 0x2101:
			mmio_w2101(data); // OBSEL
			return;
		case 0x2102:
			mmio_w2102(data); // OAMADDL
			return;
		case 0x2103:
			mmio_w2103(data); // OAMADDH
			return;
		case 0x2104:
			mmio_w2104(data); // OAMDATA
			return;
		case 0x2105:
			mmio_w2105(data); // BGMODE
			return;
		case 0x2106:
			mmio_w2106(data); // MOSAIC
			return;
		case 0x2107:
			mmio_w2107(data); // BG1SC
			return;
		case 0x2108:
			mmio_w2108(data); // BG2SC
			return;
		case 0x2109:
			mmio_w2109(data); // BG3SC
			return;
		case 0x210a:
			mmio_w210a(data); // BG4SC
			return;
		case 0x210b:
			mmio_w210b(data); // BG12NBA
			return;
		case 0x210c:
			mmio_w210c(data); // BG34NBA
			return;
		case 0x210d:
			mmio_w210d(data); // BG1HOFS
			return;
		case 0x210e:
			mmio_w210e(data); // BG1VOFS
			return;
		case 0x210f:
			mmio_w210f(data); // BG2HOFS
			return;
		case 0x2110:
			mmio_w2110(data); // BG2VOFS
			return;
		case 0x2111:
			mmio_w2111(data); // BG3HOFS
			return;
		case 0x2112:
			mmio_w2112(data); // BG3VOFS
			return;
		case 0x2113:
			mmio_w2113(data); // BG4HOFS
			return;
		case 0x2114:
			mmio_w2114(data); // BG4VOFS
			return;
		case 0x2115:
			mmio_w2115(data); // VMAIN
			return;
		case 0x2116:
			mmio_w2116(data); // VMADDL
			return;
		case 0x2117:
			mmio_w2117(data); // VMADDH
			return;
		case 0x2118:
			mmio_w2118(data); // VMDATAL
			return;
		case 0x2119:
			mmio_w2119(data); // VMDATAH
			return;
		case 0x211a:
			mmio_w211a(data); // M7SEL
			return;
		case 0x211b:
			mmio_w211b(data); // M7A
			return;
		case 0x211c:
			mmio_w211c(data); // M7B
			return;
		case 0x211d:
			mmio_w211d(data); // M7C
			return;
		case 0x211e:
			mmio_w211e(data); // M7D
			return;
		case 0x211f:
			mmio_w211f(data); // M7X
			return;
		case 0x2120:
			mmio_w2120(data); // M7Y
			return;
		case 0x2121:
			mmio_w2121(data); // CGADD
			return;
		case 0x2122:
			mmio_w2122(data); // CGDATA
			return;
		case 0x2123:
			mmio_w2123(data); // W12SEL
			return;
		case 0x2124:
			mmio_w2124(data); // W34SEL
			return;
		case 0x2125:
			mmio_w2125(data); // WOBJSEL
			return;
		case 0x2126:
			mmio_w2126(data); // WH0
			return;
		case 0x2127:
			mmio_w2127(data); // WH1
			return;
		case 0x2128:
			mmio_w2128(data); // WH2
			return;
		case 0x2129:
			mmio_w2129(data); // WH3
			return;
		case 0x212a:
			mmio_w212a(data); // WBGLOG
			return;
		case 0x212b:
			mmio_w212b(data); // WOBJLOG
			return;
		case 0x212c:
			mmio_w212c(data); // TM
			return;
		case 0x212d:
			mmio_w212d(data); // TS
			return;
		case 0x212e:
			mmio_w212e(data); // TMW
			return;
		case 0x212f:
			mmio_w212f(data); // TSW
			return;
		case 0x2130:
			mmio_w2130(data); // CGWSEL
			return;
		case 0x2131:
			mmio_w2131(data); // CGADDSUB
			return;
		case 0x2132:
			mmio_w2132(data); // COLDATA
			return;
		case 0x2133:
			mmio_w2133(data); // SETINI
			return;
		}
	}

	Background bg1;
	Background bg2;
	Background bg3;
	Background bg4;
	Sprite sprite;
	Window window;
	Screen screen;

	ByteBuffer output;

	private int ppu1_version = 1;
	private int ppu2_version = 3;

	Display display;

	final void add_clocks(int clocks)
	{
		clocks >>= 1;
		while ((clocks--) != 0)
		{
			counter.tick(2);
			clock += 2;
			// synchronize_cpu();
		}
	}

	private final void scanline()
	{
		if (counter.vcounter() == 0)
		{
			frame();
			// bg1.frame();
			// bg2.frame();
			// bg3.frame();
			// bg4.frame();
		}

		bg1.scanline();
		bg2.scanline();
		bg3.scanline();
		bg4.scanline();
		sprite.scanline();
		window.scanline();
		screen.scanline();
	}

	private final void frame()
	{
		sprite.frame();

		display.interlace = regs.interlace;
		display.overscan = regs.overscan;
	}

	@Override
	public Object readConfig(String key)
	{
		switch (key)
		{
		case "accuracy":
			return accuracy;
		default:
			return null;
		}
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		switch (key)
		{
		case "region":
			region = counter.region = value.toString().equals("ntsc") ? NTSC : PAL;
			break;
		case "ppu1 version":
			ppu1_version = (int) value;
			break;
		case "ppu2 version":
			ppu2_version = (int) value;
			break;
		case "accuracy":
			requestAccuracy = (boolean) value;
			break;
		}
	}

	HVCounter counter;

	@Override
	public void readDMA(int address, ByteBuffer data, int offset, int length)
	{
		System.arraycopy(output.array(), 0, data.array(), offset, length);
	}

	@Override
	public void writeDMA(int address, ByteBuffer data, int offset, int length)
	{
	}
}
