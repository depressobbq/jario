package jario.snes.cpu;

public class DMA
{
	private Pipe pipe = new Pipe();
	private CPU self;

	public DMA(CPU self)
	{
		this.self = self;
	}

	void dma_add_clocks(int clocks)
	{
		self.status.dma_clocks += clocks;
		self.add_clocks(clocks);
	}

	boolean dma_transfer_valid(int bbus, int abus)
	{   // transfers from WRAM to WRAM are invalid; chip only has one address bus
		if (bbus == 0x80 && ((abus & 0xfe0000) == 0x7e0000 || (abus & 0x40e000) == 0x0000)) { return false; }
		return true;
	}

	boolean dma_addr_valid(int abus)
	{   // A-bus access to B-bus or S-CPU registers are invalid
		if ((abus & 0x40ff00) == 0x2100) { return false; } // $[00-3f|80-bf]:[2100-21ff]
		if ((abus & 0x40fe00) == 0x4000) { return false; } // $[00-3f|80-bf]:[4000-41ff]}
		if ((abus & 0x40ffe0) == 0x4200) { return false; } // $[00-3f|80-bf]:[4200-421f]
		if ((abus & 0x40ff80) == 0x4300) { return false; } // $[00-3f|80-bf]:[4300-437f]
		return true;
	}

	byte dma_read(int abus)
	{
		if (dma_addr_valid(abus) == false) { return 0x00; }
		return self.bus.read8bit(abus);
	}

	void dma_write(boolean valid)
	{
		dma_write(valid, 0, (byte) 0);
	}

	void dma_write(boolean valid, int addr, byte data)
	{
		if (pipe.valid)
		{
			self.bus.write8bit(pipe.addr, pipe.data);
		}
		pipe.valid = valid;
		pipe.addr = addr;
		pipe.data = data;
	}

	void dma_transfer(boolean direction, int bbus, int abus)
	{
		if (direction == false)
		{
			dma_add_clocks(4);
			self.regs.mdr = dma_read(abus);
			dma_add_clocks(4);
			dma_write(dma_transfer_valid(bbus, abus), 0x2100 | bbus, self.regs.mdr);
		}
		else
		{
			dma_add_clocks(4);
			self.regs.mdr = dma_transfer_valid(bbus, abus) ? self.bus.read8bit(0x2100 | (bbus & 0xFF)) : 0x00;
			dma_add_clocks(4);
			dma_write(dma_addr_valid(abus), abus, self.regs.mdr);
		}
	}

	int dma_bbus(int i, int index)
	{
		switch (self.channel[i].transfer_mode)
		{
		default:
		case 0:
			return (self.channel[i].dest_addr); // 0
		case 1:
			return (self.channel[i].dest_addr + (index & 1)); // 0,1
		case 2:
			return (self.channel[i].dest_addr); // 0,0
		case 3:
			return (self.channel[i].dest_addr + ((index >> 1) & 1)); // 0,0,1,1
		case 4:
			return (self.channel[i].dest_addr + (index & 3)); // 0,1,2,3
		case 5:
			return (self.channel[i].dest_addr + (index & 1)); // 0,1,0,1
		case 6:
			return (self.channel[i].dest_addr); // 0,0 [2]
		case 7:
			return (self.channel[i].dest_addr + ((index >> 1) & 1)); // 0,0,1,1
																		// [3]
		}
	}

	int dma_addr(int i)
	{
		int r = ((self.channel[i].source_bank & 0xFF) << 16) | (self.channel[i].source_addr & 0xFFFF);

		if (self.channel[i].fixed_transfer == false)
		{
			if (self.channel[i].reverse_transfer == false)
			{
				self.channel[i].source_addr++;
			}
			else
			{
				self.channel[i].source_addr--;
			}
		}

		return r;
	}

	int hdma_addr(int i)
	{
		return ((self.channel[i].source_bank & 0xFF) << 16) | (self.channel[i].hdma_addr++ & 0xFFFF);
	}

	int hdma_iaddr(int i)
	{
		return ((self.channel[i].indirect_bank & 0xFF) << 16) | (self.channel[i].indirect_addr++ & 0xFFFF);
	}

	int dma_enabled_channels()
	{
		int r = 0;
		for (int i = 0; i < 8; i++)
		{
			if (self.channel[i].dma_enabled)
			{
				r++;
			}
		}
		return r;
	}

	boolean hdma_active(int i)
	{
		return (self.channel[i].hdma_enabled && !self.channel[i].hdma_completed);
	}

	boolean hdma_active_after(int i)
	{
		for (int n = i + 1; n < 8; n++)
		{
			if (hdma_active(n) == true) { return true; }
		}
		return false;
	}

	int hdma_enabled_channels()
	{
		int r = 0;
		for (int i = 0; i < 8; i++)
		{
			if (self.channel[i].hdma_enabled)
			{
				r++;
			}
		}
		return r;
	}

	int hdma_active_channels()
	{
		int r = 0;
		for (int i = 0; i < 8; i++)
		{
			if (hdma_active(i) == true)
			{
				r++;
			}
		}
		return r;
	}

	void dma_run()
	{
		dma_add_clocks(8);
		dma_write(false);
		self.dma_edge();

		for (int i = 0; i < 8; i++)
		{
			if (self.channel[i].dma_enabled == false)
			{
				continue;
			}

			int index = 0;
			do
			{
				dma_transfer(self.channel[i].direction, dma_bbus(i, index++), dma_addr(i));
				self.dma_edge();
			} while (self.channel[i].dma_enabled && (self.channel[i].transfer_size_decremented() & 0xFFFF) != 0);

			dma_add_clocks(8);
			dma_write(false);
			self.dma_edge();

			self.channel[i].dma_enabled = false;
		}

		self.status.irq_lock = true;
	}

	void hdma_update(int i)
	{
		dma_add_clocks(4);
		self.regs.mdr = dma_read(((self.channel[i].source_bank & 0xFF) << 16) | (self.channel[i].hdma_addr & 0xFFFF));
		dma_add_clocks(4);
		dma_write(false);

		if ((self.channel[i].line_counter & 0x7f) == 0)
		{
			self.channel[i].line_counter = self.regs.mdr;
			self.channel[i].hdma_addr++;

			self.channel[i].hdma_completed = ((self.channel[i].line_counter & 0xFF) == 0);
			self.channel[i].hdma_do_transfer = !self.channel[i].hdma_completed;

			if (self.channel[i].indirect)
			{
				dma_add_clocks(4);
				self.regs.mdr = dma_read(hdma_addr(i));
				self.channel[i].indirect_addr = (self.regs.mdr << 8) & 0xFFFF;
				dma_add_clocks(4);
				dma_write(false);

				if (!self.channel[i].hdma_completed || hdma_active_after(i))
				{
					dma_add_clocks(4);
					self.regs.mdr = dma_read(hdma_addr(i));
					self.channel[i].indirect_addr = self.channel[i].indirect_addr >> 8;
					self.channel[i].indirect_addr = self.channel[i].indirect_addr | ((self.regs.mdr << 8) & 0xFFFF);
					dma_add_clocks(4);
					dma_write(false);
				}
			}
		}
	}

	private static int[] transfer_length = { 1, 2, 2, 4, 4, 4, 2, 4 };

	void hdma_run()
	{
		dma_add_clocks(8);
		dma_write(false);

		for (int i = 0; i < 8; i++)
		{
			if (hdma_active(i) == false)
			{
				continue;
			}
			self.channel[i].dma_enabled = false; // HDMA run during DMA will stop DMA mid-transfer

			if (self.channel[i].hdma_do_transfer)
			{
				int length = transfer_length[self.channel[i].transfer_mode];
				for (int index = 0; index < length; index++)
				{
					int addr = self.channel[i].indirect == false ? hdma_addr(i) : hdma_iaddr(i);
					dma_transfer(self.channel[i].direction, dma_bbus(i, index), addr);
				}
			}
		}

		for (int i = 0; i < 8; i++)
		{
			if (hdma_active(i) == false)
			{
				continue;
			}

			self.channel[i].line_counter--;
			self.channel[i].hdma_do_transfer = (self.channel[i].line_counter & 0x80) != 0;
			hdma_update(i);
		}

		self.status.irq_lock = true;
	}

	void hdma_init_reset()
	{
		for (int i = 0; i < 8; i++)
		{
			self.channel[i].hdma_completed = false;
			self.channel[i].hdma_do_transfer = false;
		}
	}

	void hdma_init()
	{
		dma_add_clocks(8);
		dma_write(false);

		for (int i = 0; i < 8; i++)
		{
			if (!self.channel[i].hdma_enabled)
			{
				continue;
			}
			self.channel[i].dma_enabled = false; // HDMA init during DMA will stop DMA mid-transfer

			self.channel[i].hdma_addr = self.channel[i].source_addr & 0xFFFF;
			self.channel[i].line_counter = 0;
			hdma_update(i);
		}

		self.status.irq_lock = true;
	}

	void dma_power()
	{
		for (int i = 0; i < 8; i++)
		{
			self.channel[i].direction = true;
			self.channel[i].indirect = true;
			self.channel[i].unused = true;
			self.channel[i].reverse_transfer = true;
			self.channel[i].fixed_transfer = true;
			self.channel[i].transfer_mode = 0x7;

			self.channel[i].dest_addr = 0xff;

			self.channel[i].source_addr = 0xffff;
			self.channel[i].source_bank = 0xff;

			self.channel[i].transfer_size(0xffff);
			self.channel[i].indirect_bank = 0xff;

			self.channel[i].hdma_addr = 0xffff;
			self.channel[i].line_counter = 0xff;
			self.channel[i].unknown = 0xff;
		}
	}

	void dma_reset()
	{
		for (int i = 0; i < 8; i++)
		{
			self.channel[i].dma_enabled = false;
			self.channel[i].hdma_enabled = false;

			self.channel[i].hdma_completed = false;
			self.channel[i].hdma_do_transfer = false;
		}

		pipe.valid = false;
		pipe.addr = 0;
		pipe.data = 0;
	}
}
