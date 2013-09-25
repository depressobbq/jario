package jario.snes.dsp;

import jario.hardware.Bus32bit;
import jario.hardware.Bus8bit;
import jario.hardware.Hardware;

public abstract class DSPCore implements Hardware
{
	protected Bus8bit apuram;
	protected Bus32bit audio;

	interface DSPOp
	{
		public void run();
	}

	public DSPOp op_0 = new DSPOp()
	{
		public void run()
		{
			voice_5(voice[0]);
			voice_2(voice[1]);
		}
	};

	public DSPOp op_1 = new DSPOp()
	{
		public void run()
		{
			voice_6(voice[0]);
			voice_3(voice[1]);
		}
	};

	public DSPOp op_2 = new DSPOp()
	{
		public void run()
		{
			voice_7(voice[0]);
			voice_4(voice[1]);
			voice_1(voice[3]);
		}
	};

	public DSPOp op_3 = new DSPOp()
	{
		public void run()
		{
			voice_8(voice[0]);
			voice_5(voice[1]);
			voice_2(voice[2]);
		}
	};

	public DSPOp op_4 = new DSPOp()
	{
		public void run()
		{
			voice_9(voice[0]);
			voice_6(voice[1]);
			voice_3(voice[2]);
		}
	};

	public DSPOp op_5 = new DSPOp()
	{
		public void run()
		{
			voice_7(voice[1]);
			voice_4(voice[2]);
			voice_1(voice[4]);
		}
	};

	public DSPOp op_6 = new DSPOp()
	{
		public void run()
		{
			voice_8(voice[1]);
			voice_5(voice[2]);
			voice_2(voice[3]);
		}
	};

	public DSPOp op_7 = new DSPOp()
	{
		public void run()
		{
			voice_9(voice[1]);
			voice_6(voice[2]);
			voice_3(voice[3]);
		}
	};

	public DSPOp op_8 = new DSPOp()
	{
		public void run()
		{
			voice_7(voice[2]);
			voice_4(voice[3]);
			voice_1(voice[5]);
		}
	};

	public DSPOp op_9 = new DSPOp()
	{
		public void run()
		{
			voice_8(voice[2]);
			voice_5(voice[3]);
			voice_2(voice[4]);
		}
	};

	public DSPOp op_10 = new DSPOp()
	{
		public void run()
		{
			voice_9(voice[2]);
			voice_6(voice[3]);
			voice_3(voice[4]);
		}
	};

	public DSPOp op_11 = new DSPOp()
	{
		public void run()
		{
			voice_7(voice[3]);
			voice_4(voice[4]);
			voice_1(voice[6]);
		}
	};

	public DSPOp op_12 = new DSPOp()
	{
		public void run()
		{
			voice_8(voice[3]);
			voice_5(voice[4]);
			voice_2(voice[5]);
		}
	};

	public DSPOp op_13 = new DSPOp()
	{
		public void run()
		{
			voice_9(voice[3]);
			voice_6(voice[4]);
			voice_3(voice[5]);
		}
	};

	public DSPOp op_14 = new DSPOp()
	{
		public void run()
		{
			voice_7(voice[4]);
			voice_4(voice[5]);
			voice_1(voice[7]);
		}
	};

	public DSPOp op_15 = new DSPOp()
	{
		public void run()
		{
			voice_8(voice[4]);
			voice_5(voice[5]);
			voice_2(voice[6]);
		}
	};

	public DSPOp op_16 = new DSPOp()
	{
		public void run()
		{
			voice_9(voice[4]);
			voice_6(voice[5]);
			voice_3(voice[6]);
		}
	};

	public DSPOp op_17 = new DSPOp()
	{
		public void run()
		{
			voice_1(voice[0]);
			voice_7(voice[5]);
			voice_4(voice[6]);
		}
	};

	public DSPOp op_18 = new DSPOp()
	{
		public void run()
		{
			voice_8(voice[5]);
			voice_5(voice[6]);
			voice_2(voice[7]);
		}
	};

	public DSPOp op_19 = new DSPOp()
	{
		public void run()
		{
			voice_9(voice[5]);
			voice_6(voice[6]);
			voice_3(voice[7]);
		}
	};

	public DSPOp op_20 = new DSPOp()
	{
		public void run()
		{
			voice_1(voice[1]);
			voice_7(voice[6]);
			voice_4(voice[7]);
		}
	};

	public DSPOp op_21 = new DSPOp()
	{
		public void run()
		{
			voice_8(voice[6]);
			voice_5(voice[7]);
			voice_2(voice[0]);
		}
	};

	public DSPOp op_22 = new DSPOp()
	{
		public void run()
		{
			voice_3a(voice[0]);
			voice_9(voice[6]);
			voice_6(voice[7]);
			echo_22();
		}
	};

	public DSPOp op_23 = new DSPOp()
	{
		public void run()
		{
			voice_7(voice[7]);
			echo_23();
		}
	};

	public DSPOp op_24 = new DSPOp()
	{
		public void run()
		{
			voice_8(voice[7]);
			echo_24();
		}
	};

	public DSPOp op_25 = new DSPOp()
	{
		public void run()
		{
			voice_3b(voice[0]);
			voice_9(voice[7]);
			echo_25();
		}
	};

	public DSPOp op_26 = new DSPOp()
	{
		public void run()
		{
			echo_26();
		}
	};

	public DSPOp op_27 = new DSPOp()
	{
		public void run()
		{
			misc_27();
			echo_27();
		}
	};

	public DSPOp op_28 = new DSPOp()
	{
		public void run()
		{
			misc_28();
			echo_28();
		}
	};

	public DSPOp op_29 = new DSPOp()
	{
		public void run()
		{
			misc_29();
			echo_29();
		}
	};

	public DSPOp op_30 = new DSPOp()
	{
		public void run()
		{
			misc_30();
			voice_3c(voice[0]);
			echo_30();
		}
	};

	public DSPOp op_31 = new DSPOp()
	{
		public void run()
		{
			voice_4(voice[0]);
			voice_1(voice[2]);
		}
	};

	protected DSPOp[] ops = new DSPOp[32];
	protected int op;

	void initOps()
	{
		ops[0] = op_0;
		ops[1] = op_1;
		ops[2] = op_2;
		ops[3] = op_3;
		ops[4] = op_4;
		ops[5] = op_5;
		ops[6] = op_6;
		ops[7] = op_7;
		ops[8] = op_8;
		ops[9] = op_9;
		ops[10] = op_10;
		ops[11] = op_11;
		ops[12] = op_12;
		ops[13] = op_13;
		ops[14] = op_14;
		ops[15] = op_15;
		ops[16] = op_16;
		ops[17] = op_17;
		ops[18] = op_18;
		ops[19] = op_19;
		ops[20] = op_20;
		ops[21] = op_21;
		ops[22] = op_22;
		ops[23] = op_23;
		ops[24] = op_24;
		ops[25] = op_25;
		ops[26] = op_26;
		ops[27] = op_27;
		ops[28] = op_28;
		ops[29] = op_29;
		ops[30] = op_30;
		ops[31] = op_31;
	}

	public static final int GlobalReg_mvoll = 0x0c;
	public static final int GlobalReg_mvolr = 0x1c;
	public static final int GlobalReg_evoll = 0x2c;
	public static final int GlobalReg_evolr = 0x3c;
	public static final int GlobalReg_kon = 0x4c;
	public static final int GlobalReg_koff = 0x5c;
	public static final int GlobalReg_flg = 0x6c;
	public static final int GlobalReg_endx = 0x7c;
	public static final int GlobalReg_efb = 0x0d;
	public static final int GlobalReg_pmon = 0x2d;
	public static final int GlobalReg_non = 0x3d;
	public static final int GlobalReg_eon = 0x4d;
	public static final int GlobalReg_dir = 0x5d;
	public static final int GlobalReg_esa = 0x6d;
	public static final int GlobalReg_edl = 0x7d;
	public static final int GlobalReg_fir = 0x0f;
	// voice registers
	public static final int VoiceReg_voll = 0x00;
	public static final int VoiceReg_volr = 0x01;
	public static final int VoiceReg_pitchl = 0x02;
	public static final int VoiceReg_pitchh = 0x03;
	public static final int VoiceReg_srcn = 0x04;
	public static final int VoiceReg_adsr0 = 0x05;
	public static final int VoiceReg_adsr1 = 0x06;
	public static final int VoiceReg_gain = 0x07;
	public static final int VoiceReg_envx = 0x08;
	public static final int VoiceReg_outx = 0x09;

	// internal envelope modes
	public static final int EnvMode_release = 0;
	public static final int EnvMode_attack = 1;
	public static final int EnvMode_decay = 2;
	public static final int EnvMode_sustain = 3;

	// internal constants
	public static final int echo_hist_size = 8;
	public static final int brr_buf_size = 12;
	public static final int brr_block_size = 9;

	protected State state = new State(echo_hist_size);

	// voice state
	protected Voice[] voice = new Voice[8];

	// gaussian
	private static short[] gaussian_table =
	{
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
			2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5,
			6, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10,
			11, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 15, 16, 16, 17, 17,
			18, 19, 19, 20, 20, 21, 21, 22, 23, 23, 24, 24, 25, 26, 27, 27,
			28, 29, 29, 30, 31, 32, 32, 33, 34, 35, 36, 36, 37, 38, 39, 40,
			41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
			58, 59, 60, 61, 62, 64, 65, 66, 67, 69, 70, 71, 73, 74, 76, 77,
			78, 80, 81, 83, 84, 86, 87, 89, 90, 92, 94, 95, 97, 99, 100, 102,
			104, 106, 107, 109, 111, 113, 115, 117, 118, 120, 122, 124, 126, 128, 130, 132,
			134, 137, 139, 141, 143, 145, 147, 150, 152, 154, 156, 159, 161, 163, 166, 168,
			171, 173, 175, 178, 180, 183, 186, 188, 191, 193, 196, 199, 201, 204, 207, 210,
			212, 215, 218, 221, 224, 227, 230, 233, 236, 239, 242, 245, 248, 251, 254, 257,
			260, 263, 267, 270, 273, 276, 280, 283, 286, 290, 293, 297, 300, 304, 307, 311,
			314, 318, 321, 325, 328, 332, 336, 339, 343, 347, 351, 354, 358, 362, 366, 370,
			374, 378, 381, 385, 389, 393, 397, 401, 405, 410, 414, 418, 422, 426, 430, 434,
			439, 443, 447, 451, 456, 460, 464, 469, 473, 477, 482, 486, 491, 495, 499, 504,
			508, 513, 517, 522, 527, 531, 536, 540, 545, 550, 554, 559, 563, 568, 573, 577,
			582, 587, 592, 596, 601, 606, 611, 615, 620, 625, 630, 635, 640, 644, 649, 654,
			659, 664, 669, 674, 678, 683, 688, 693, 698, 703, 708, 713, 718, 723, 728, 732,
			737, 742, 747, 752, 757, 762, 767, 772, 777, 782, 787, 792, 797, 802, 806, 811,
			816, 821, 826, 831, 836, 841, 846, 851, 855, 860, 865, 870, 875, 880, 884, 889,
			894, 899, 904, 908, 913, 918, 923, 927, 932, 937, 941, 946, 951, 955, 960, 965,
			969, 974, 978, 983, 988, 992, 997, 1001, 1005, 1010, 1014, 1019, 1023, 1027, 1032, 1036,
			1040, 1045, 1049, 1053, 1057, 1061, 1066, 1070, 1074, 1078, 1082, 1086, 1090, 1094, 1098, 1102,
			1106, 1109, 1113, 1117, 1121, 1125, 1128, 1132, 1136, 1139, 1143, 1146, 1150, 1153, 1157, 1160,
			1164, 1167, 1170, 1174, 1177, 1180, 1183, 1186, 1190, 1193, 1196, 1199, 1202, 1205, 1207, 1210,
			1213, 1216, 1219, 1221, 1224, 1227, 1229, 1232, 1234, 1237, 1239, 1241, 1244, 1246, 1248, 1251,
			1253, 1255, 1257, 1259, 1261, 1263, 1265, 1267, 1269, 1270, 1272, 1274, 1275, 1277, 1279, 1280,
			1282, 1283, 1284, 1286, 1287, 1288, 1290, 1291, 1292, 1293, 1294, 1295, 1296, 1297, 1297, 1298,
			1299, 1300, 1300, 1301, 1302, 1302, 1303, 1303, 1303, 1304, 1304, 1304, 1304, 1304, 1305, 1305,
	};

	private final int gaussian_interpolate(Voice v)
	{   // make pointers into gaussian table based on fractional position between
		// samples
		int offset = (v.interp_pos >> 4) & 0xff;
		int fwd = 255 - offset;
		int rev = offset; // mirror left half of gaussian table

		offset = v.buf_pos + (v.interp_pos >> 12);
		int output;
		output = (gaussian_table[fwd + 0] * v.buffer.read(offset + 0)) >> 11;
		output += (gaussian_table[fwd + 256] * v.buffer.read(offset + 1)) >> 11;
		output += (gaussian_table[rev + 256] * v.buffer.read(offset + 2)) >> 11;
		output = (short) output;
		output += (gaussian_table[rev + 0] * v.buffer.read(offset + 3)) >> 11;
		return sclamp(16, output) & ~1;
	}

	// counter
	private static final int counter_range = 2048 * 5 * 3; // 30720 (0x7800)

	private static int[] counter_rate =
	{
			0, 2048, 1536,
			1280, 1024, 768,
			640, 512, 384,
			320, 256, 192,
			160, 128, 96,
			80, 64, 48,
			40, 32, 24,
			20, 16, 12,
			10, 8, 6,
			5, 4, 3,
			2,
			1,
	};

	private static int[] counter_offset =
	{
			0, 0, 1040,
			536, 0, 1040,
			536, 0, 1040,
			536, 0, 1040,
			536, 0, 1040,
			536, 0, 1040,
			536, 0, 1040,
			536, 0, 1040,
			536, 0, 1040,
			536, 0, 1040,
			0,
			0,
	};

	private void counter_tick()
	{
		state.counter--;
		if (state.counter < 0)
		{
			state.counter = counter_range - 1;
		}
	}

	private boolean counter_poll(int rate)
	{
		if (rate == 0) { return false; }
		return ((state.counter + counter_offset[rate]) % counter_rate[rate]) == 0;
	}

	// envelope
	private void envelope_run(Voice v)
	{
		int env = v.env;

		if (v.env_mode == EnvMode_release)
		{ // 60%
			env -= 0x8;
			if (env < 0)
			{
				env = 0;
			}
			v.env = env;
			return;
		}

		int rate;
		int env_data = state.regs[v.vidx + VoiceReg_adsr1] & 0xFF;
		if ((state.t_adsr0 & 0x80) != 0)
		{ // 99% ADSR
			if (v.env_mode >= EnvMode_decay)
			{ // 99%
				env--;
				env -= env >> 8;
				rate = env_data & 0x1f;
				if (v.env_mode == EnvMode_decay)
				{ // 1%
					rate = ((state.t_adsr0 >> 3) & 0x0e) + 0x10;
				}
			}
			else
			{ // env_attack
				rate = ((state.t_adsr0 & 0x0f) << 1) + 1;
				env += rate < 31 ? 0x20 : 0x400;
			}
		}
		else
		{ // GAIN
			env_data = state.regs[v.vidx + VoiceReg_gain] & 0xFF;
			int mode = env_data >> 5;
			if (mode < 4)
			{ // direct
				env = env_data << 4;
				rate = 31;
			}
			else
			{
				rate = env_data & 0x1f;
				if (mode == 4)
				{ // 4: linear decrease
					env -= 0x20;
				}
				else if (mode < 6)
				{ // 5: exponential decrease
					env--;
					env -= env >> 8;
				}
				else
				{ // 6, 7: linear increase
					env += 0x20;
					if (mode > 6 && (v.hidden_env & 0xFFFFFFFFL) >= 0x600L)
					{
						env += 0x8 - 0x20; // 7: two-slope linear increase
					}
				}
			}
		}

		// sustain level
		if ((env >> 8) == (env_data >> 5) && v.env_mode == EnvMode_decay)
		{
			v.env_mode = EnvMode_sustain;
		}
		v.hidden_env = env;

		// unsigned cast because linear decrease underflowing also triggers this
		if ((env & 0xFFFFFFFFL) > 0x7ffL)
		{
			env = (env < 0 ? 0 : 0x7ff);
			if (v.env_mode == EnvMode_attack)
			{
				v.env_mode = EnvMode_decay;
			}
		}

		if (counter_poll(rate) == true)
		{
			v.env = env;
		}
	}

	// brr
	private final void brr_decode(Voice v)
	{
		int nybbles = (state.t_brr_byte << 8) + (apuram.read8bit((v.brr_addr + v.brr_offset + 1) & 0xFFFF) & 0xFF);

		int filter = (state.t_brr_header >> 2) & 3;
		int scale = (state.t_brr_header >> 4);

		// decode four samples
		for (int i = 0; i < 4; i++)
		{
			// bits 12-15 = current nybble; sign extend, then shift right to
			// 4-bit precision
			// result: s = 4-bit sign-extended sample value
			int s = (short) nybbles >> 12;
			nybbles <<= 4; // slide nybble so that on next loop iteration, bits
							// 12-15 = current nybble

			if (scale <= 12)
			{
				s <<= scale;
				s >>= 1;
			}
			else
			{
				s &= ~0x7ff;
			}

			// apply IIR filter (2 is the most commonly used)
			int p1 = v.buffer.read(v.buf_pos - 1);
			int p2 = v.buffer.read(v.buf_pos - 2) >> 1;

			switch (filter)
			{
			case 0:
				break; // no filter
			case 1:
			{
				// s += p1 * 0.46875
				s += p1 >> 1;
				s += (-p1) >> 5;
			}
				break;
			case 2:
			{
				// s += p1 * 0.953125 - p2 * 0.46875
				s += p1;
				s -= p2;
				s += p2 >> 4;
				s += (p1 * -3) >> 6;
			}
				break;
			case 3:
			{
				// s += p1 * 0.8984375 - p2 * 0.40625
				s += p1;
				s -= p2;
				s += (p1 * -13) >> 7;
				s += (p2 * 3) >> 4;
			}
				break;
			}

			// adjust and write sample
			s = sclamp(16, s);
			s = (short) (s << 1);
			v.buffer.write(v.buf_pos++, s);
			if (v.buf_pos >= brr_buf_size)
			{
				v.buf_pos = 0;
			}
		}
	}

	// misc
	private void misc_27()
	{
		// voice 0 doesn't support PMON
		state.t_pmon = (state.regs[GlobalReg_pmon] & 0xFF) & ~1;
	}

	private void misc_28()
	{
		state.t_non = state.regs[GlobalReg_non] & 0xFF;
		state.t_eon = state.regs[GlobalReg_eon] & 0xFF;
		state.t_dir = state.regs[GlobalReg_dir] & 0xFF;
	}

	private void misc_29()
	{
		state.every_other_sample = !state.every_other_sample;
		if (state.every_other_sample)
		{
			// clears KON 63 clocks after it was last read
			state.new_kon &= ~state.kon;
		}
	}

	private void misc_30()
	{
		if (state.every_other_sample)
		{
			state.kon = state.new_kon;
			state.t_koff = state.regs[GlobalReg_koff] & 0xFF;
		}

		counter_tick();

		// noise
		if (counter_poll(state.regs[GlobalReg_flg] & 0x1f) == true)
		{
			int feedback = (state.noise << 13) ^ (state.noise << 14);
			state.noise = (feedback & 0x4000) ^ (state.noise >> 1);
		}
	}

	// voice
	private void voice_output(Voice v, boolean ch)
	{
		int channel = ch ? 1 : 0;

		// apply left/right volume
		int amp = (state.t_output * (byte) (state.regs[v.vidx + VoiceReg_voll + channel] & 0xFF)) >> 7;

		// add to output total
		state.t_main_out[channel] += amp;
		state.t_main_out[channel] = sclamp(16, state.t_main_out[channel]);

		// optionally add to echo total
		if ((state.t_eon & v.vbit) != 0)
		{
			state.t_echo_out[channel] += amp;
			state.t_echo_out[channel] = sclamp(16, state.t_echo_out[channel]);
		}
	}

	private void voice_1(Voice v)
	{
		state.t_dir_addr = (state.t_dir << 8) + (state.t_srcn << 2);
		state.t_srcn = state.regs[v.vidx + VoiceReg_srcn] & 0xFF;
	}

	private void voice_2(Voice v)
	{ // read sample pointer (ignored if not needed)
		int addr = state.t_dir_addr & 0xFFFF;
		if ((v.kon_delay) == 0)
		{
			addr += 2;
		}
		int lo = apuram.read8bit((addr + 0)) & 0xFF;
		int hi = apuram.read8bit((addr + 1)) & 0xFF;
		state.t_brr_next_addr = ((hi << 8) + lo);

		state.t_adsr0 = state.regs[v.vidx + VoiceReg_adsr0] & 0xFF;

		// read pitch, spread over two clocks
		state.t_pitch = state.regs[v.vidx + VoiceReg_pitchl] & 0xFF;
	}

	private final void voice_3(Voice v)
	{
		voice_3a(v);
		voice_3b(v);
		voice_3c(v);
	}

	private final void voice_3a(Voice v)
	{
		state.t_pitch += (state.regs[v.vidx + VoiceReg_pitchh] & 0x3f) << 8;
	}

	private final void voice_3b(Voice v)
	{
		state.t_brr_byte = apuram.read8bit((v.brr_addr + v.brr_offset) & 0xFFFF) & 0xFF;
		state.t_brr_header = apuram.read8bit((v.brr_addr) & 0xFFFF) & 0xFF;
	}

	private final void voice_3c(Voice v)
	{ // pitch modulation using previous voice's output

		if ((state.t_pmon & v.vbit) != 0)
		{
			state.t_pitch += ((state.t_output >> 5) * state.t_pitch) >> 10;
		}

		if ((v.kon_delay) != 0)
		{
			// get ready to start BRR decoding on next sample
			if (v.kon_delay == 5)
			{
				v.brr_addr = state.t_brr_next_addr;
				v.brr_offset = 1;
				v.buf_pos = 0;
				state.t_brr_header = 0; // header is ignored on this sample
			}

			// envelope is never run during KON
			v.env = 0;
			v.hidden_env = 0;

			// disable BRR decoding until last three samples
			v.interp_pos = 0;
			v.kon_delay--;
			if ((v.kon_delay & 3) != 0)
			{
				v.interp_pos = 0x4000;
			}

			// pitch is never added during KON
			state.t_pitch = 0;
		}

		// gaussian interpolation
		int output = gaussian_interpolate(v);

		// noise
		if ((state.t_non & v.vbit) != 0)
		{
			output = (short) (state.noise << 1);
		}

		// apply envelope
		state.t_output = ((output * v.env) >> 11) & ~1;
		v.t_envx_out = v.env >> 4;

		// immediate silence due to end of sample or soft reset
		if ((state.regs[GlobalReg_flg] & 0x80) != 0 || (state.t_brr_header & 3) == 1)
		{
			v.env_mode = EnvMode_release;
			v.env = 0;
		}

		if (state.every_other_sample)
		{
			// KOFF
			if ((state.t_koff & v.vbit) != 0)
			{
				v.env_mode = EnvMode_release;
			}

			// KON
			if ((state.kon & v.vbit) != 0)
			{
				v.kon_delay = 5;
				v.env_mode = EnvMode_attack;
			}
		}

		// run envelope for next sample
		if ((v.kon_delay) == 0)
		{
			envelope_run(v);
		}
	}

	private void voice_4(Voice v)
	{ // decode BRR
		state.t_looped = 0;
		if (v.interp_pos >= 0x4000)
		{
			brr_decode(v);
			v.brr_offset += 2;
			if (v.brr_offset >= 9)
			{
				// start decoding next BRR block
				v.brr_addr = (v.brr_addr + 9) & 0xFFFF;
				if ((state.t_brr_header & 1) != 0)
				{
					v.brr_addr = state.t_brr_next_addr;
					state.t_looped = v.vbit;
				}
				v.brr_offset = 1;
			}
		}

		// apply pitch
		v.interp_pos = (v.interp_pos & 0x3fff) + state.t_pitch;

		// keep from getting too far ahead (when using pitch modulation)
		if (v.interp_pos > 0x7fff)
		{
			v.interp_pos = 0x7fff;
		}

		// output left
		voice_output(v, false);
	}

	private void voice_5(Voice v)
	{ // output right
		voice_output(v, true);

		// ENDX, OUTX and ENVX won't update if you wrote to them 1-2 clocks earlier
		state.endx_buf = (state.regs[GlobalReg_endx] & 0xFF) | state.t_looped;

		// clear bit in ENDX if KON just began
		if (v.kon_delay == 5)
		{
			state.endx_buf &= ~v.vbit;
		}
	}

	private void voice_6(Voice v)
	{
		state.outx_buf = state.t_output >> 8;
	}

	private void voice_7(Voice v)
	{ // update ENDX
		state.regs[GlobalReg_endx] = state.endx_buf & 0xFF;
		state.envx_buf = v.t_envx_out;
	}

	private void voice_8(Voice v)
	{ // update OUTX
		state.regs[v.vidx + VoiceReg_outx] = state.outx_buf & 0xFF;
	}

	private void voice_9(Voice v)
	{ // update ENVX
		state.regs[v.vidx + VoiceReg_envx] = state.envx_buf & 0xFF;
	}

	// echo
	private int calc_fir(int i, boolean ch)
	{
		int channel = ch ? 1 : 0;
		int s = state.echo_hist[channel].read(state.echo_hist_pos + i + 1);
		return (s * (byte) (state.regs[GlobalReg_fir + i * 0x10] & 0xFF)) >> 6;
	}

	private int echo_output(boolean ch)
	{
		int channel = ch ? 1 : 0;
		int output = (short) ((state.t_main_out[channel] * (byte) (state.regs[GlobalReg_mvoll + channel * 0x10] & 0xFF)) >> 7)
				+ (short) ((state.t_echo_in[channel] * (byte) (state.regs[GlobalReg_evoll + channel * 0x10] & 0xFF)) >> 7);
		return sclamp(16, output);
	}

	private void echo_read(boolean ch)
	{
		int channel = ch ? 1 : 0;
		int addr = (state.t_echo_ptr + channel * 2);
		int lo = apuram.read8bit((addr + 0) & 0xFFFF) & 0xFF;
		int hi = apuram.read8bit((addr + 1) & 0xFFFF) & 0xFF;
		int s = (short) ((hi << 8) + lo);
		state.echo_hist[channel].write(state.echo_hist_pos, s >> 1);
	}

	private void echo_write(boolean ch)
	{
		int channel = ch ? 1 : 0;
		if ((state.t_echo_disabled & 0x20) == 0)
		{
			int addr = (state.t_echo_ptr + channel * 2);
			int s = state.t_echo_out[channel];
			apuram.write8bit((addr + 0) & 0xFFFF, (byte) (s & 0xFF));
			apuram.write8bit((addr + 1) & 0xFFFF, (byte) ((s >> 8) & 0xFF));
		}

		state.t_echo_out[channel] = 0;
	}

	private void echo_22()
	{ // history
		state.echo_hist_pos++;
		if (state.echo_hist_pos >= echo_hist_size)
		{
			state.echo_hist_pos = 0;
		}

		state.t_echo_ptr = ((state.t_esa << 8) + state.echo_offset) & 0xFFFF;
		echo_read(false);

		// FIR
		int l = calc_fir(0, false);
		int r = calc_fir(0, true);

		state.t_echo_in[0] = l;
		state.t_echo_in[1] = r;
	}

	private void echo_23()
	{
		int l = calc_fir(1, false) + calc_fir(2, false);
		int r = calc_fir(1, true) + calc_fir(2, true);

		state.t_echo_in[0] += l;
		state.t_echo_in[1] += r;

		echo_read(true);
	}

	private void echo_24()
	{
		int l = calc_fir(3, false) + calc_fir(4, false) + calc_fir(5, false);
		int r = calc_fir(3, true) + calc_fir(4, true) + calc_fir(5, true);

		state.t_echo_in[0] += l;
		state.t_echo_in[1] += r;
	}

	private void echo_25()
	{
		int l = state.t_echo_in[0] + calc_fir(6, false);
		int r = state.t_echo_in[1] + calc_fir(6, true);

		l = (short) l;
		r = (short) r;

		l += (short) calc_fir(7, false);
		r += (short) calc_fir(7, true);

		state.t_echo_in[0] = sclamp(16, l) & ~1;
		state.t_echo_in[1] = sclamp(16, r) & ~1;
	}

	private void echo_26()
	{ // left output volumes
		// (save sample for next clock so we can output both together)
		state.t_main_out[0] = echo_output(false);

		// echo feedback
		int l = state.t_echo_out[0] + (short) ((state.t_echo_in[0] * (byte) (state.regs[GlobalReg_efb] & 0xFF)) >> 7);
		int r = state.t_echo_out[1] + (short) ((state.t_echo_in[1] * (byte) (state.regs[GlobalReg_efb] & 0xFF)) >> 7);

		state.t_echo_out[0] = sclamp(16, l) & ~1;
		state.t_echo_out[1] = sclamp(16, r) & ~1;
	}

	private void echo_27()
	{ // output
		int outl = state.t_main_out[0];
		int outr = echo_output(true);
		state.t_main_out[0] = 0;
		state.t_main_out[1] = 0;

		// global muting isn't this simple
		// (turns DAC on and off or something, causing small ~37-sample pulse
		// when first muted)
		if ((state.regs[GlobalReg_flg] & 0x40) != 0)
		{
			outl = 0;
			outr = 0;
		}

		// output sample to DAC
		audio.write32bit(0, (outl << 16) | (outr & 0xFFFF));
	}

	private void echo_28()
	{
		state.t_echo_disabled = state.regs[GlobalReg_flg] & 0xFF;
	}

	private void echo_29()
	{
		state.t_esa = state.regs[GlobalReg_esa] & 0xFF;

		if ((state.echo_offset) == 0)
		{
			state.echo_length = (state.regs[GlobalReg_edl] & 0x0f) << 11;
		}

		state.echo_offset += 4;
		if (state.echo_offset >= state.echo_length)
		{
			state.echo_offset = 0;
		}

		// write left echo
		echo_write(false);

		state.t_echo_disabled = state.regs[GlobalReg_flg] & 0xFF;
	}

	private void echo_30()
	{ // write right echo
		echo_write(true);
	}

	public DSPCore()
	{
		initOps();
	}

	private static final int sclamp(int bits, int x)
	{
		int b = 1 << (bits - 1);
		int m = (1 << (bits - 1)) - 1;
		return ((x > m) ? m : (x < -b) ? -b : x);
	}
}
