/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.cpu;

import jario.hardware.Bus1bit;
import jario.hardware.Bus32bit;
import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.util.Arrays;

public class CPU extends CPUCore implements Hardware, Clockable, Bus1bit, Bus8bit, Configurable
{
	public static final int NTSC = 0;
	public static final int PAL = 1;
	
	protected Bus8bit bus;
	protected Clockable smp;
	protected Clockable ppu;
	protected Bus1bit ppu1bit;
	protected Bus8bit smp_bus;
	protected Bus8bit input_bus;
	protected Bus32bit video_bus;

	private long clock;
	private int ticks;
	private int stage;
	private int addr;

	private Clockable coprocessors;

	Channel[] channel = new Channel[8];
	DMA dma;
	private int cpu_version;
	Status status = new BusB();
	private ALU alu = new ALU();
	HVCounter counter;

	public CPU()
	{
		initialize_opcode_table();

		for (int i = 0; i < channel.length; i++)
		{
			channel[i] = new Channel();
		}

		dma = new DMA(this);
		
		counter = new HVCounter();
		counter.scanline = this.scanline;
		
		power();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			bus = (Bus8bit) hw;
			break;
		case 1:
			smp = (Clockable) hw;
			smp_bus = (Bus8bit) hw;
			break;
		case 2:
			input_bus = (Bus8bit) hw;
			break;
		case 3:
			video_bus = (Bus32bit) hw;
			break;
		case 4:
			ppu = (Clockable) hw;
			ppu1bit = (Bus1bit) hw;
			counter.ppu1bit = ppu1bit;
			break;
		case 5:
			coprocessors = (Clockable) hw;
			break;
		}
	}

	@Override
	public boolean interrupt_pending()
	{
		return status.interrupt_pending;
	}

	@Override
	public final void clock(long clocks)
	{
		while (clocks-- > 0L)
		{
			if (ticks-- > 0)
			{
				counter.tick();
				if ((counter.hcounter() & 2) != 0)
				{
					// Input.input.tick();
					poll_interrupts();
				}
				clock += 2;
				ppu.clock(2);
				if (coprocessors != null)
				{
					coprocessors.clock(2);
				}

				if (ticks == 0 && !status.dram_refreshed && counter.hcounter() >= status.dram_refresh_position)
				{
					status.dram_refreshed = true;
					status.irq_lock = false;
					ticks = 40 >> 1;
				}
			}
			else
			{
				switch (stage)
				{
				case 0:
					if (status.interrupt_pending)
					{
						status.interrupt_pending = false;
						if (status.nmi_pending)
						{
							status.nmi_pending = false;
							status.interrupt_vector = regs.e == false ? 0xffea : 0xfffa;
							op_irq();
						}
						else if (status.irq_pending)
						{
							status.irq_pending = false;
							status.interrupt_vector = regs.e == false ? 0xffee : 0xfffe;
							op_irq();
						}
						else if (status.reset_pending)
						{
							status.reset_pending = false;
							add_clocks(186);
							regs.pc.l(bus.read8bit(0xfffc) & 0xFF);
							regs.pc.h(bus.read8bit(0xfffd) & 0xFF);
							System.out.println("reset pending: " + Integer.toHexString(regs.pc.w()));
						}
					}
					addr = (regs.pc.b() << 16) + regs.pc.w();
					regs.pc.w(regs.pc.w() + 1);
					status.clock_count = speed(addr);
					dma_edge();
					status.irq_lock = false;
					ticks = (status.clock_count - 4) >> 1;
					stage = 1;
					break;
				case 1:
					regs.mdr = bus.read8bit(addr);
					status.irq_lock = false;
					ticks = 4 >> 1;
					stage = 2;
					break;
				case 2:
					alu_edge();
					int op = regs.mdr & 0xFF;
					opcode_table.get(op).Invoke();
					stage = 0;
					break;
				}
			}

			smp.clock(clock);
			clock = 0;
		}
	}

	@Override
	public void reset()
	{
		stage = 0;
		clock = 0;

		// should this remove the coprocessors or reset them?
		//coprocessors = null;
		counter.reset();

		// note: some registers are not fully reset by SNES
		regs.pc.set(0x000000);
		regs.x.h(0x00);
		regs.y.h(0x00);
		regs.s.h(0x01);
		regs.d.set(0x0000);
		regs.db = 0x00;
		regs.p.set(0x34);
		regs.e = true;
		regs.mdr = 0x00;
		regs.wai = false;
		update_table();

		if (status != null)
		{
			mmio_reset();
			dma.dma_reset();
			timing_reset();
		}
	}

	@Override
	public byte read8bit(int addr)
	{
		addr &= 0xffff;

		// APU
		if ((addr & 0xffc0) == 0x2140)
		{ // $2140-$217f
			// synchronize_smp();
			return smp_bus.read8bit(addr);
		}

		// DMA
		if ((addr & 0xff80) == 0x4300)
		{ // $4300-$437f
			int i = (addr >> 4) & 7;
			switch (addr & 0xf)
			{
			case 0x0:
				return mmio_r43x0(i);
			case 0x1:
				return mmio_r43x1(i);
			case 0x2:
				return mmio_r43x2(i);
			case 0x3:
				return mmio_r43x3(i);
			case 0x4:
				return mmio_r43x4(i);
			case 0x5:
				return mmio_r43x5(i);
			case 0x6:
				return mmio_r43x6(i);
			case 0x7:
				return mmio_r43x7(i);
			case 0x8:
				return mmio_r43x8(i);
			case 0x9:
				return mmio_r43x9(i);
			case 0xa:
				return mmio_r43xa(i);
			case 0xb:
				return mmio_r43xb(i);
			case 0xc:
				return regs.mdr; // unmapped
			case 0xd:
				return regs.mdr; // unmapped
			case 0xe:
				return regs.mdr; // unmapped
			case 0xf:
				return mmio_r43xb(i); // mirror of $43xb
			}
		}

		switch (addr)
		{
		case 0x2180:
			return mmio_r2180();
		case 0x4016:
			return mmio_r4016();
		case 0x4017:
			return mmio_r4017();
		case 0x4210:
			return mmio_r4210();
		case 0x4211:
			return mmio_r4211();
		case 0x4212:
			return mmio_r4212();
		case 0x4213:
			return mmio_r4213();
		case 0x4214:
			return mmio_r4214();
		case 0x4215:
			return mmio_r4215();
		case 0x4216:
			return mmio_r4216();
		case 0x4217:
			return mmio_r4217();
		case 0x4218:
			return mmio_r4218();
		case 0x4219:
			return mmio_r4219();
		case 0x421a:
			return mmio_r421a();
		case 0x421b:
			return mmio_r421b();
		case 0x421c:
			return mmio_r421c();
		case 0x421d:
			return mmio_r421d();
		case 0x421e:
			return mmio_r421e();
		case 0x421f:
			return mmio_r421f();
		}

		return regs.mdr;
	}

	@Override
	public void write8bit(int addr, byte data)
	{
		addr &= 0xffff;

		// APU
		if ((addr & 0xffc0) == 0x2140)
		{ // $2140-$217f
			// synchronize_smp();
			((Bus8bit) status).write8bit(addr, data);
			return;
		}

		// DMA
		if ((addr & 0xff80) == 0x4300)
		{ // $4300-$437f
			int i = (addr >> 4) & 7;
			switch (addr & 0xf)
			{
			case 0x0:
				mmio_w43x0(i, data);
				return;
			case 0x1:
				mmio_w43x1(i, data);
				return;
			case 0x2:
				mmio_w43x2(i, data);
				return;
			case 0x3:
				mmio_w43x3(i, data);
				return;
			case 0x4:
				mmio_w43x4(i, data);
				return;
			case 0x5:
				mmio_w43x5(i, data);
				return;
			case 0x6:
				mmio_w43x6(i, data);
				return;
			case 0x7:
				mmio_w43x7(i, data);
				return;
			case 0x8:
				mmio_w43x8(i, data);
				return;
			case 0x9:
				mmio_w43x9(i, data);
				return;
			case 0xa:
				mmio_w43xa(i, data);
				return;
			case 0xb:
				mmio_w43xb(i, data);
				return;
			case 0xc:
				return; // unmapped
			case 0xd:
				return; // unmapped
			case 0xe:
				return; // unmapped
			case 0xf:
				mmio_w43xb(i, data);
				return; // mirror of $43xb
			}
		}

		switch (addr)
		{
		case 0x2180:
			mmio_w2180(data);
			return;
		case 0x2181:
			mmio_w2181(data);
			return;
		case 0x2182:
			mmio_w2182(data);
			return;
		case 0x2183:
			mmio_w2183(data);
			return;
		case 0x4016:
			mmio_w4016(data);
			return;
		case 0x4017:
			return; // unmapped
		case 0x4200:
			mmio_w4200(data);
			return;
		case 0x4201:
			mmio_w4201(data);
			return;
		case 0x4202:
			mmio_w4202(data);
			return;
		case 0x4203:
			mmio_w4203(data);
			return;
		case 0x4204:
			mmio_w4204(data);
			return;
		case 0x4205:
			mmio_w4205(data);
			return;
		case 0x4206:
			mmio_w4206(data);
			return;
		case 0x4207:
			mmio_w4207(data);
			return;
		case 0x4208:
			mmio_w4208(data);
			return;
		case 0x4209:
			mmio_w4209(data);
			return;
		case 0x420a:
			mmio_w420a(data);
			return;
		case 0x420b:
			mmio_w420b(data);
			return;
		case 0x420c:
			mmio_w420c(data);
			return;
		case 0x420d:
			mmio_w420d(data);
			return;
		}
	}

	@Override
	public Object readConfig(String key)
	{
		if (key.equals("BUS B")) return status;
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("cpu version")) cpu_version = (Integer) value;
		if (key.equals("region")) counter.region = value.toString().equals("ntsc") ? NTSC : PAL;
	}

	@Override
	public void op_io()
	{
		status.clock_count = 6;
		dma_edge();
		add_clocks(6);
		alu_edge();
	}

	@Override
	public byte op_read(int addr)
	{
		status.clock_count = speed(addr);
		dma_edge();
		add_clocks(status.clock_count - 4);
		regs.mdr = bus.read8bit(addr);
		add_clocks(4);
		alu_edge();
		return regs.mdr;
	}

	@Override
	public void op_write(int addr, byte data)
	{
		alu_edge();
		status.clock_count = speed(addr);
		dma_edge();
		add_clocks(status.clock_count);
		bus.write8bit(addr, (regs.mdr = data));
	}

	@Override
	public void last_cycle()
	{
		if (status.irq_lock == false)
		{
			status.nmi_pending |= nmi_test();
			status.irq_pending |= irq_test();
			status.interrupt_pending = (status.nmi_pending || status.irq_pending);
		}
	}

	private void power()
	{
		regs.a.set(0x0000);
		regs.x.set(0x0000);
		regs.y.set(0x0000);
		regs.s.set(0x01ff);

		dma.dma_power();

		reset();
	}

	private int speed(int addr)
	{
		if ((addr & 0x408000) != 0)
		{
			if ((addr & 0x800000) != 0) { return status.rom_speed; }
			return 8;
		}
		if (((addr + 0x6000) & 0x4000) != 0) { return 8; }
		if (((addr - 0x4000) & 0x7e00) != 0) { return 6; }
		return 12;
	}

	private void mmio_reset()
	{
		// $2140-217f
		Arrays.fill(status.port, (byte) 0);

		// $2181-$2183
		status.wram_addr = 0x000000;

		// $4016-$4017
		status.joypad1_bits = ~0;
		status.joypad2_bits = ~0;

		// $4200
		status.nmi_enabled = false;
		status.hirq_enabled = false;
		status.virq_enabled = false;
		status.auto_joypad_poll = false;

		// $4201
		status.pio = 0xff;

		// $4202-$4203
		status.wrmpya = 0xff;
		status.wrmpyb = 0xff;

		// $4204-$4206
		status.wrdiva = 0xffff;
		status.wrdivb = 0xff;

		// $4207-$420a
		status.hirq_pos = 0x01ff;
		status.virq_pos = 0x01ff;

		// $420d
		status.rom_speed = 8;

		// $4214-$4217
		status.rddiv = 0x0000;
		status.rdmpy = 0x0000;

		// $4218-$421f
		status.joy1l = 0x00;
		status.joy1h = 0x00;
		status.joy2l = 0x00;
		status.joy2h = 0x00;
		status.joy3l = 0x00;
		status.joy3h = 0x00;
		status.joy4l = 0x00;
		status.joy4h = 0x00;

		// ALU
		alu.mpyctr = 0;
		alu.divctr = 0;
		alu.shift = 0;
	}

	private byte mmio_r2180()
	{
		return bus.read8bit(0x7e0000 | ((status.wram_addr++) & 0x1FFFF));
	}

	private byte mmio_r4016()
	{
		int r = regs.mdr & 0xfc;
		r |= (input_bus.read8bit(0) & 3);
		return (byte) r;
	}

	private byte mmio_r4017()
	{
		int r = (regs.mdr & 0xe0) | 0x1c;
		r |= (input_bus.read8bit(1) & 3);
		return (byte) r;
	}

	private byte mmio_r4210()
	{
		int r = regs.mdr & 0x70;
		r |= ((rdnmi() ? 1 : 0) << 7);
		r |= (cpu_version & 0x0f);
		return (byte) r;
	}

	private byte mmio_r4211()
	{
		int r = regs.mdr & 0x7f;
		r |= ((timeup() ? 1 : 0) << 7);
		return (byte) r;
	}

	private byte mmio_r4212()
	{
		int r = regs.mdr & 0x3e;
		int vs = (!ppu1bit.read1bit(1) ? 225 : 240);
		if (counter.vcounter() >= vs && counter.vcounter() <= (vs + 2))
		{
			r |= 0x01; // auto joypad polling
		}
		if (counter.hcounter() <= 2 || counter.hcounter() >= 1096)
		{
			r |= 0x40; // hblank
		}
		if (counter.vcounter() >= vs)
		{
			r |= 0x80; // vblank
		}
		return (byte) r;
	}

	private byte mmio_r4213()
	{
		return (byte) status.pio;
	}

	private byte mmio_r4214()
	{
		return (byte) (status.rddiv >> 0);
	}

	private byte mmio_r4215()
	{
		return (byte) (status.rddiv >> 8);
	}

	private byte mmio_r4216()
	{
		return (byte) (status.rdmpy >> 0);
	}

	private byte mmio_r4217()
	{
		return (byte) (status.rdmpy >> 8);
	}

	private byte mmio_r4218()
	{
		return (byte) status.joy1l;
	}

	private byte mmio_r4219()
	{
		return (byte) status.joy1h;
	}

	private byte mmio_r421a()
	{
		return (byte) status.joy2l;
	}

	private byte mmio_r421b()
	{
		return (byte) status.joy2h;
	}

	private byte mmio_r421c()
	{
		return (byte) status.joy3l;
	}

	private byte mmio_r421d()
	{
		return (byte) status.joy3h;
	}

	private byte mmio_r421e()
	{
		return (byte) status.joy4l;
	}

	private byte mmio_r421f()
	{
		return (byte) status.joy4h;
	}

	private byte mmio_r43x0(int i)
	{
		return (byte) (((channel[i].direction ? 1 : 0) << 7)
				| ((channel[i].indirect ? 1 : 0) << 6)
				| ((channel[i].unused ? 1 : 0) << 5)
				| ((channel[i].reverse_transfer ? 1 : 0) << 4)
				| ((channel[i].fixed_transfer ? 1 : 0) << 3)
				| (channel[i].transfer_mode << 0));
	}

	private byte mmio_r43x1(int i)
	{
		return (byte) channel[i].dest_addr;
	}

	private byte mmio_r43x2(int i)
	{
		return (byte) (channel[i].source_addr >> 0);
	}

	private byte mmio_r43x3(int i)
	{
		return (byte) (channel[i].source_addr >> 8);
	}

	private byte mmio_r43x4(int i)
	{
		return (byte) channel[i].source_bank;
	}

	private byte mmio_r43x5(int i)
	{
		return (byte) (channel[i].transfer_size() >> 0);
	}

	private byte mmio_r43x6(int i)
	{
		return (byte) (channel[i].transfer_size() >> 8);
	}

	private byte mmio_r43x7(int i)
	{
		return (byte) channel[i].indirect_bank;
	}

	private byte mmio_r43x8(int i)
	{
		return (byte) (channel[i].hdma_addr >> 0);
	}

	private byte mmio_r43x9(int i)
	{
		return (byte) (channel[i].hdma_addr >> 8);
	}

	private byte mmio_r43xa(int i)
	{
		return (byte) channel[i].line_counter;
	}

	private byte mmio_r43xb(int i)
	{
		return (byte) channel[i].unknown;
	}

	private void mmio_w2180(byte data)
	{
		bus.write8bit(0x7e0000 | ((status.wram_addr++) & 0x1FFFF), data);
	}

	private void mmio_w2181(byte data)
	{
		status.wram_addr = (status.wram_addr & 0x01ff00) | ((data & 0xFF) << 0);
	}

	private void mmio_w2182(byte data)
	{
		status.wram_addr = (status.wram_addr & 0x0100ff) | ((data & 0xFF) << 8);
	}

	private void mmio_w2183(byte data)
	{
		status.wram_addr = (status.wram_addr & 0x00ffff) | ((data & 0xFF) << 16);
	}

	private void mmio_w4016(byte data)
	{
		input_bus.write8bit(0, data);
	}

	private void mmio_w4200(byte data)
	{
		status.auto_joypad_poll = (data & 1) != 0;
		nmitimen_update(data);
	}

	private void mmio_w4201(byte data)
	{
		ppu1bit.write1bit(29, ((data >> 7) & 0x1) != 0);
		status.pio = data & 0xFF;
	}

	private void mmio_w4202(byte data)
	{
		status.wrmpya = data & 0xFF;
	}

	private void mmio_w4203(byte data)
	{
		status.rdmpy = 0;
		if (alu.mpyctr != 0 || alu.divctr != 0) { return; }

		status.wrmpyb = data & 0xFF;
		status.rddiv = (status.wrmpyb << 8) | status.wrmpya;

		alu.mpyctr = 8; // perform multiplication over the next eight cycles
		alu.shift = status.wrmpyb;
	}

	private void mmio_w4204(byte data)
	{
		status.wrdiva = (status.wrdiva & 0xff00) | ((data & 0xFF) << 0);
	}

	private void mmio_w4205(byte data)
	{
		status.wrdiva = (status.wrdiva & 0x00ff) | ((data & 0xFF) << 8);
	}

	private void mmio_w4206(byte data)
	{
		status.rdmpy = status.wrdiva;
		if (alu.mpyctr != 0 || alu.divctr != 0) { return; }

		status.wrdivb = data & 0xFF;

		alu.divctr = 16; // perform division over the next sixteen cycles
		alu.shift = status.wrdivb << 16;
	}

	private void mmio_w4207(byte data)
	{
		status.hirq_pos = (status.hirq_pos & 0x0100) | ((data & 0xFF) << 0);
	}

	private void mmio_w4208(byte data)
	{
		status.hirq_pos = (status.hirq_pos & 0x00ff) | ((data & 0xFF) << 8);
	}

	private void mmio_w4209(byte data)
	{
		status.virq_pos = (status.virq_pos & 0x0100) | ((data & 0xFF) << 0);
	}

	private void mmio_w420a(byte data)
	{
		status.virq_pos = (status.virq_pos & 0x00ff) | ((data & 0xFF) << 8);
	}

	private void mmio_w420b(byte data)
	{
		for (int i = 0; i < 8; i++)
		{
			channel[i].dma_enabled = (data & (1 << i)) != 0;
		}
		if (data != 0)
		{
			status.dma_pending = true;
		}
	}

	private void mmio_w420c(byte data)
	{
		for (int i = 0; i < 8; i++)
		{
			channel[i].hdma_enabled = (data & (1 << i)) != 0;
		}
	}

	private void mmio_w420d(byte data)
	{
		status.rom_speed = ((data & 1) != 0 ? 6 : 8);
	}

	private void mmio_w43x0(int i, byte data)
	{
		channel[i].direction = (data & 0x80) != 0;
		channel[i].indirect = (data & 0x40) != 0;
		channel[i].unused = (data & 0x20) != 0;
		channel[i].reverse_transfer = (data & 0x10) != 0;
		channel[i].fixed_transfer = (data & 0x08) != 0;
		channel[i].transfer_mode = data & 0x07;
	}

	private void mmio_w43x1(int i, byte data)
	{
		channel[i].dest_addr = data & 0xFF;
	}

	private void mmio_w43x2(int i, byte data)
	{
		channel[i].source_addr = (channel[i].source_addr & 0xff00) | ((data & 0xFF) << 0);
	}

	private void mmio_w43x3(int i, byte data)
	{
		channel[i].source_addr = (channel[i].source_addr & 0x00ff) | ((data & 0xFF) << 8);
	}

	private void mmio_w43x4(int i, byte data)
	{
		channel[i].source_bank = data & 0xFF;
	}

	private void mmio_w43x5(int i, byte data)
	{
		channel[i].transfer_size((channel[i].transfer_size() & 0xff00) | ((data & 0xFF) << 0));
	}

	private void mmio_w43x6(int i, byte data)
	{
		channel[i].transfer_size((channel[i].transfer_size() & 0x00ff) | ((data & 0xFF) << 8));
	}

	private void mmio_w43x7(int i, byte data)
	{
		channel[i].indirect_bank = data & 0xFF;
	}

	private void mmio_w43x8(int i, byte data)
	{
		channel[i].hdma_addr = (channel[i].hdma_addr & 0xff00) | ((data & 0xFF) << 0);
	}

	private void mmio_w43x9(int i, byte data)
	{
		channel[i].hdma_addr = (channel[i].hdma_addr & 0x00ff) | ((data & 0xFF) << 8);
	}

	private void mmio_w43xa(int i, byte data)
	{
		channel[i].line_counter = data & 0xFF;
	}

	private void mmio_w43xb(int i, byte data)
	{
		channel[i].unknown = data & 0xFF;
	}

	private int dma_counter()
	{
		return (status.dma_counter + counter.hcounter()) & 7;
	}

	final void add_clocks(int clocks)
	{
		status.irq_lock = false;
		ticks = clocks >> 1;

		while ((ticks--) != 0)
		{
			counter.tick();
			if ((counter.hcounter() & 2) != 0)
			{
				// Input.input.tick();
				poll_interrupts();
			}
			clock += 2;
			ppu.clock(2);
		}
		if (coprocessors != null)
		{
			coprocessors.clock(clocks);
		}
		if (!status.dram_refreshed && counter.hcounter() >= status.dram_refresh_position)
		{
			status.dram_refreshed = true;
			add_clocks(40);
		}
	}

	Runnable scanline = new Runnable()
	{
		public void run()
		{
			status.dma_counter = (status.dma_counter + status.line_clocks) & 7;
			status.line_clocks = counter.lineclocks();

			// forcefully sync S-CPU to other processors, in case chips are not
			// communicating
			// synchronize_ppu();
			// synchronize_smp();
			coprocessors.clock(0);

			video_bus.write32bit(0, counter.vcounter());
			if (counter.vcounter() == 241)
			{
				((Clockable) input_bus).clock(0L);
				((Clockable) video_bus).clock(0L);
			}

			if (counter.vcounter() == 0)
			{
				// HDMA init triggers once every frame
				status.hdma_init_position = (cpu_version == 1 ? 12 + 8 - dma_counter() : 12 + dma_counter());
				status.hdma_init_triggered = false;
			}

			// DRAM refresh occurs once every scanline
			if (cpu_version == 2)
			{
				status.dram_refresh_position = 530 + 8 - dma_counter();
			}
			status.dram_refreshed = false;

			// HDMA triggers once every visible scanline
			if (counter.vcounter() <= (!ppu1bit.read1bit(1) ? 224 : 239))
			{
				status.hdma_position = 1104;
				status.hdma_triggered = false;
			}

			if (status.auto_joypad_poll == true && counter.vcounter() == (!ppu1bit.read1bit(1) ? 227 : 242))
			{
				input_bus.write8bit(1, (byte) 0); // poll
				run_auto_joypad_poll();
			}
		}
	};

	private void alu_edge()
	{
		if (alu.mpyctr != 0)
		{
			alu.mpyctr--;
			if ((status.rddiv & 1) != 0)
			{
				status.rdmpy = status.rdmpy + alu.shift;
			}
			status.rddiv = status.rddiv >> 1;
			alu.shift <<= 1;
		}

		if (alu.divctr != 0)
		{
			alu.divctr--;
			status.rddiv = status.rddiv << 1;
			alu.shift >>= 1;
			if (status.rdmpy >= alu.shift)
			{
				status.rdmpy = status.rdmpy - alu.shift;
				status.rddiv = status.rddiv | 1;
			}
		}
	}

	void dma_edge()
	{   // H/DMA pending && DMA inactive?
		// .. Run one full CPU cycle
		// .. HDMA pending && HDMA enabled ? DMA sync + HDMA run
		// .. DMA pending && DMA enabled ? DMA sync + DMA run
		// .... HDMA during DMA && HDMA enabled ? DMA sync + HDMA run
		// .. Run one bus CPU cycle
		// .. CPU sync

		if (status.dma_active == true)
		{
			if (status.hdma_pending)
			{
				status.hdma_pending = false;
				if (dma.hdma_enabled_channels() != 0)
				{
					if (dma.dma_enabled_channels() == 0)
					{
						dma.dma_add_clocks(8 - dma_counter());
					}
					if (status.hdma_mode == false)
					{
						dma.hdma_init();
					}
					else
					{
						dma.hdma_run();
					}
					if (dma.dma_enabled_channels() == 0)
					{
						add_clocks(status.clock_count - (status.dma_clocks % status.clock_count));
						status.dma_active = false;
					}
				}
			}

			if (status.dma_pending)
			{
				status.dma_pending = false;
				if (dma.dma_enabled_channels() != 0)
				{
					dma.dma_add_clocks(8 - dma_counter());
					dma.dma_run();
					add_clocks(status.clock_count - (status.dma_clocks % status.clock_count));
					status.dma_active = false;
				}
			}
		}

		if (status.hdma_init_triggered == false && counter.hcounter() >= status.hdma_init_position)
		{
			status.hdma_init_triggered = true;
			dma.hdma_init_reset();
			if (dma.hdma_enabled_channels() != 0)
			{
				status.hdma_pending = true;
				status.hdma_mode = false;
			}
		}

		if (status.hdma_triggered == false && counter.hcounter() >= status.hdma_position)
		{
			status.hdma_triggered = true;
			if (dma.hdma_active_channels() != 0)
			{
				status.hdma_pending = true;
				status.hdma_mode = true;
			}
		}

		if (status.dma_active == false)
		{
			if (status.dma_pending || status.hdma_pending)
			{
				status.dma_clocks = 0;
				status.dma_active = true;
			}
		}
	}

	private void timing_reset()
	{
		status.clock_count = 0;
		status.line_clocks = counter.lineclocks();

		status.irq_lock = false;
		status.dram_refresh_position = (cpu_version == 1 ? 530 : 538);
		status.dram_refreshed = false;

		status.hdma_init_position = (cpu_version == 1 ? 12 + 8 - dma_counter() : 12 + dma_counter());
		status.hdma_init_triggered = false;

		status.hdma_position = 1104;
		status.hdma_triggered = false;

		status.nmi_valid = false;
		status.nmi_line = false;
		status.nmi_transition = false;
		status.nmi_pending = false;
		status.nmi_hold = false;

		status.irq_valid = false;
		status.irq_line = false;
		status.irq_transition = false;
		status.irq_pending = false;
		status.irq_hold = false;

		status.reset_pending = true;
		status.interrupt_pending = true;
		status.interrupt_vector = 0xfffc; // reset vector address

		status.dma_active = false;
		status.dma_counter = 0;
		status.dma_clocks = 0;
		status.dma_pending = false;
		status.hdma_pending = false;
		status.hdma_mode = false;
	}

	private final void poll_interrupts()
	{ // NMI hold
		if (status.nmi_hold)
		{
			status.nmi_hold = false;
			if (status.nmi_enabled)
			{
				status.nmi_transition = true;
			}
		}

		// NMI test
		boolean nmi_valid = (counter.vcounter(2) >= (!ppu1bit.read1bit(1) ? 225 : 240));
		if (!status.nmi_valid && nmi_valid)
		{
			// 0->1 edge sensitive transition
			status.nmi_line = true;
			status.nmi_hold = true; // hold /NMI for four cycles
		}
		else if (status.nmi_valid && !nmi_valid)
		{
			// 1->0 edge sensitive transition
			status.nmi_line = false;
		}
		status.nmi_valid = nmi_valid;

		// IRQ hold
		status.irq_hold = false;
		if (status.irq_line)
		{
			if (status.virq_enabled || status.hirq_enabled)
			{
				status.irq_transition = true;
			}
		}

		// IRQ test
		boolean irq_valid = (status.virq_enabled || status.hirq_enabled);
		if (irq_valid)
		{
			// IRQs cannot trigger on last dot of field
			if ((status.virq_enabled && counter.vcounter(10) != status.virq_pos)
					|| (status.hirq_enabled && counter.hcounter(10) != (status.hirq_pos + 1) * 4)
					|| ((status.virq_pos != 0) && counter.vcounter(6) == 0)
			)
			{
				irq_valid = false;
			}
		}
		if (!status.irq_valid && irq_valid)
		{
			// 0->1 edge sensitive transition
			status.irq_line = true;
			status.irq_hold = true; // hold /IRQ for four cycles
		}
		status.irq_valid = irq_valid;
	}

	private void nmitimen_update(byte data)
	{
		boolean nmi_enabled = status.nmi_enabled;
		status.nmi_enabled = (data & 0x80) != 0;
		status.virq_enabled = (data & 0x20) != 0;
		status.hirq_enabled = (data & 0x10) != 0;

		// 0->1 edge sensitive transition
		if (!nmi_enabled && status.nmi_enabled && status.nmi_line)
		{
			status.nmi_transition = true;
		}

		// ?->1 level sensitive transition
		if (status.virq_enabled && !status.hirq_enabled && status.irq_line)
		{
			status.irq_transition = true;
		}

		if (!status.virq_enabled && !status.hirq_enabled)
		{
			status.irq_line = false;
			status.irq_transition = false;
		}

		status.irq_lock = true;
	}

	private boolean rdnmi()
	{
		boolean result = status.nmi_line;
		if (!status.nmi_hold)
		{
			status.nmi_line = false;
		}
		return result;
	}

	private boolean timeup()
	{
		boolean result = status.irq_line;
		if (!status.irq_hold)
		{
			status.irq_line = false;
			status.irq_transition = false;
		}
		return result;
	}

	private boolean nmi_test()
	{
		if (!status.nmi_transition) { return false; }
		status.nmi_transition = false;
		regs.wai = false;
		return true;
	}

	private boolean irq_test()
	{
		if (!status.irq_transition && !regs.irq) { return false; }
		status.irq_transition = false;
		regs.wai = false;
		return !regs.p.i;
	}

	private void run_auto_joypad_poll()
	{
		int joy1 = 0, joy2 = 0, joy3 = 0, joy4 = 0;
		for (int i = 0; i < 16; i++)
		{
			byte port0 = input_bus.read8bit(0);
			byte port1 = input_bus.read8bit(1);

			joy1 |= (((port0 & 1) != 0) ? (0x8000 >> i) : 0);
			joy2 |= (((port1 & 1) != 0) ? (0x8000 >> i) : 0);
			joy3 |= (((port0 & 2) != 0) ? (0x8000 >> i) : 0);
			joy4 |= (((port1 & 2) != 0) ? (0x8000 >> i) : 0);
		}

		status.joy1l = (joy1 >> 0);
		status.joy1h = (joy1 >> 8);

		status.joy2l = (joy2 >> 0);
		status.joy2h = (joy2 >> 8);

		status.joy3l = (joy3 >> 0);
		status.joy3h = (joy3 >> 8);

		status.joy4l = (joy4 >> 0);
		status.joy4h = (joy4 >> 8);
	}

	private void op_irq()
	{
		op_read(regs.pc.get());
		op_io();
		if (!regs.e)
		{
			op_writestack(regs.pc.b());
		}
		op_writestack(regs.pc.h());
		op_writestack(regs.pc.l());
		op_writestack(regs.e ? (regs.p.get() & ~0x10) : regs.p.get());
		rd.l(op_read(status.interrupt_vector + 0));
		regs.pc.b(0x00);
		regs.p.i = true;
		regs.p.d = false;
		rd.h(op_read(status.interrupt_vector + 1));
		regs.pc.w(rd.w());
	}

	@Override
	public boolean read1bit(int address)
	{
		return false;
	}

	@Override
	public void write1bit(int address, boolean data)
	{
		if (address == 0)
		{
			regs.irq = data;
		}
	}
}
