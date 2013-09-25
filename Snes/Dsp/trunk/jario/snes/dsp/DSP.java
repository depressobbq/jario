package jario.snes.dsp;

import jario.hardware.Bus32bit;
import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Hardware;

public class DSP extends DSPCore implements Clockable, Bus8bit
{
	private long clock;

	public DSP()
	{
		for (int i = 0; i < voice.length; i++)
		{
			voice[i] = new Voice(brr_buf_size);
		}
		power();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
		case 0:
			apuram = (Bus8bit) hw;
			power();
			break;
		case 1:
			audio = (Bus32bit) hw;
			break;
		}
	}

	@Override
	public byte read8bit(int addr)
	{
		return (byte) state.regs[addr];
	}

	@Override
	public void write8bit(int addr, byte data)
	{
		state.regs[addr] = data & 0xFF;

		if ((addr & 0x0f) == VoiceReg_envx)
		{
			state.envx_buf = data & 0xFF;
		}
		else if ((addr & 0x0f) == VoiceReg_outx)
		{
			state.outx_buf = data & 0xFF;
		}
		else if ((addr & 0xFF) == GlobalReg_kon)
		{
			state.new_kon = data & 0xFF;
		}
		else if ((addr & 0xFF) == GlobalReg_endx)
		{
			// always cleared, regardless of data written
			state.endx_buf = 0;
			state.regs[GlobalReg_endx] = 0;
		}
	}

	@Override
	public final void clock(long clocks)
	{
		clock -= clocks;
		while (clock < 0L)
		{
			ops[op++].run();
			op &= 0x1F;
			clock += 24L; // 3 * 8
		}
	}

	@Override
	public void reset()
	{
		clock = 0;
		op = 0;

		state.regs[GlobalReg_flg] = 0xe0;
		state.noise = 0x4000;
		state.echo_hist_pos = 0;
		state.every_other_sample = true;
		state.echo_offset = 0;
		state.counter = 0;
	}

	private void power()
	{
		state.echo_hist_pos = 0;
		state.every_other_sample = false;
		state.kon = 0;
		state.noise = 0;
		state.counter = 0;
		state.echo_offset = 0;
		state.echo_length = 0;
		state.new_kon = 0;
		state.endx_buf = 0;
		state.envx_buf = 0;
		state.outx_buf = 0;
		state.t_pmon = 0;
		state.t_non = 0;
		state.t_eon = 0;
		state.t_dir = 0;
		state.t_koff = 0;
		state.t_brr_next_addr = 0;
		state.t_adsr0 = 0;
		state.t_brr_header = 0;
		state.t_brr_byte = 0;
		state.t_srcn = 0;
		state.t_esa = 0;
		state.t_echo_disabled = 0;
		state.t_dir_addr = 0;
		state.t_pitch = 0;
		state.t_output = 0;
		state.t_looped = 0;
		state.t_echo_ptr = 0;
		state.t_main_out[0] = state.t_main_out[1] = 0;
		state.t_echo_out[0] = state.t_echo_out[1] = 0;
		state.t_echo_in[0] = state.t_echo_in[1] = 0;

		for (int i = 0; i < 8; i++)
		{
			voice[i].buf_pos = 0;
			voice[i].interp_pos = 0;
			voice[i].brr_addr = 0;
			voice[i].brr_offset = 1;
			voice[i].vbit = 1 << i;
			voice[i].vidx = (i * 0x10);
			voice[i].kon_delay = 0;
			voice[i].env_mode = EnvMode_release;
			voice[i].env = 0;
			voice[i].t_envx_out = 0;
			voice[i].hidden_env = 0;
		}

		reset();
	}
}
