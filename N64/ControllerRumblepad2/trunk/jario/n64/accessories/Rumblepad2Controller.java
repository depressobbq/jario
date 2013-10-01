/**
 * Copyright 2013 Jason LaDere
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

package jario.n64.accessories;

import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Hardware;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

public class Rumblepad2Controller implements Hardware, BusDMA
{
	private static final int PAK_TYPE_NONE = 1;
	private static final int PAK_TYPE_MEMPAK = 2;
	// private static final int PAK_TYPE_RUMBLE_PAK = 3; // not implemeted for non raw data
	// private static final int PAK_TYPE_TANSFER_PAK = 4; // not implemeted for non raw data
	private static final int PAK_TYPE_RAW = 5; // the controller plugin is passed in raw data

	private static final int PAK_TYPE_REG = 0;
	private static final int PAK_CONTROL_REG = 1;

	private static final int NUMBER_OF_BUTTONS = 14;

	private static final int U_DPAD = -1;
	private static final int D_DPAD = -1;
	private static final int L_DPAD = -1;
	private static final int R_DPAD = -1;
	private static final int START_BUTTON = 9;
	private static final int A_BUTTON = 1;
	private static final int B_BUTTON = 0;
	private static final int L_TRIG = 4;
	private static final int R_TRIG = 5;
	private static final int Z_TRIG = 8;
	private static final int U_CBUTTON = 3;
	private static final int D_CBUTTON = 2;
	private static final int L_CBUTTON = 6;
	private static final int R_CBUTTON = 7;

	private static final int A_BUTTON_VALUE = 0x80000000;
	private static final int B_BUTTON_VALUE = 0x40000000;
	private static final int Z_TRIG_VALUE = 0x20000000;
	private static final int START_BUTTON_VALUE = 0x10000000;
	private static final int U_DPAD_VALUE = 0x08000000;
	private static final int D_DPAD_VALUE = 0x04000000;
	private static final int L_DPAD_VALUE = 0x02000000;
	private static final int R_DPAD_VALUE = 0x01000000;
	// private static final int RESERVED2_VALUE = 0x00800000;
	// private static final int RESERVED1_VALUE = 0x00400000;
	private static final int L_TRIG_VALUE = 0x00200000;
	private static final int R_TRIG_VALUE = 0x00100000;
	private static final int U_CBUTTON_VALUE = 0x00080000;
	private static final int D_CBUTTON_VALUE = 0x00040000;
	private static final int L_CBUTTON_VALUE = 0x00020000;
	private static final int R_CBUTTON_VALUE = 0x00010000;

	private class Key
	{
		public int vkey;
		public int value;

		public Key(int value, int vkey)
		{
			this.value = value;
			this.vkey = vkey;
		}
	}

	Runnable poll = new Runnable()
	{
		public void run()
		{
			while (true)
			{
				try
				{
					Thread.sleep(100);
				}
				catch (Exception e)
				{
				}
				;

				Controllers.poll();

				update();
			}
		}
	};

	Controller controller;
	int buttonCount;
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	public Rumblepad2Controller()
	{
		System.out.println("Joypad init");

		pakType = PAK_TYPE_NONE;
	}

	private void update()
	{
		for (int i = 0; i < keys.length; i++)
		{
			if (keys[i].vkey >= 0)
			{
				if (controller.isButtonPressed(keys[i].vkey))
				{
					controllerButtons |= keys[i].value;
				}
				else
				{
					controllerButtons &= ~keys[i].value;
				}
			}
		}
		for (int i = buttonCount; i < buttonCount + controller.getAxisCount(); i++)
		{
			if (controller.getAxisName(i - buttonCount).equals("X Axis"))
			{
				float value = controller.getAxisValue(i - buttonCount);

				if (value > 0.5f && value < 1.0f)
				{
					controllerButtons |= keys[3].value;
				}
				else if (value < -0.5f && value > -1.0f)
				{
					controllerButtons |= keys[2].value;
				}
				else
				{
					controllerButtons &= ~keys[3].value;
					controllerButtons &= ~keys[2].value;
				}
			}
			if (controller.getAxisName(i - buttonCount).equals("Y Axis"))
			{
				float value = controller.getAxisValue(i - buttonCount);

				if (value > 0.5f && value < 1.0f)
				{
					controllerButtons |= keys[1].value;
				}
				else if (value < -0.5f && value > -1.0f)
				{
					controllerButtons |= keys[0].value;
				}
				else
				{
					controllerButtons &= ~keys[1].value;
					controllerButtons &= ~keys[0].value;
				}
			}
		}
	}

	private int controllerButtons;
	private Key[] keys = new Key[NUMBER_OF_BUTTONS];
	private int pakType;
	private boolean config;

	private Bus32bit expansionslot;

	@Override
	public void connect(int port, Hardware bus)
	{
		switch (port)
		{
		case 0:
			expansionslot = (Bus32bit) bus;
			if (expansionslot != null)
				pakType = expansionslot.read32bit(PAK_TYPE_REG);
			else
				pakType = PAK_TYPE_NONE;
			break;
		}
	}

	@Override
	public void reset()
	{
		config = false;
		if (expansionslot != null)
		{
			((Hardware) expansionslot).reset();
		}
	}

	@Override
	public void readDMA(int pAddr, ByteBuffer dmaObj, int offset, int length)
	{
		if (!config)
			config(pAddr);
		byte[] dma = dmaObj.array();
		switch (dma[offset + 2] & 0xFF)
		{
		case 0x01: // read controller
			dma[offset + 3] = (byte) (controllerButtons >> 24);
			dma[offset + 4] = (byte) (controllerButtons >> 16);
			dma[offset + 5] = (byte) (controllerButtons >> 8);
			dma[offset + 6] = (byte) (controllerButtons);
			break;
		case 0x02: // read from controller pack
			switch (pakType)
			{
			case PAK_TYPE_RAW:
			{
				break;
			}
			}
			break;
		case 0x03: // write controller pak
			switch (pakType)
			{
			case PAK_TYPE_RAW:
			{
				break;
			}
			}
			break;
		}
	}

	@Override
	public void writeDMA(int pAddr, ByteBuffer dmaObj, int offset, int length)
	{
		if (!config)
			config(pAddr);
		int address;
		byte[] dma = dmaObj.array();
		switch (dma[offset + 2] & 0xFF)
		{
		case 0x00: // check
		case 0xFF: // reset & check ?
			if ((dma[offset + 1] & 0x80) != 0)
				break;
			dma[offset + 3] = (byte) 0x05;
			dma[offset + 4] = (byte) 0x00;
			switch (pakType)
			{
			case PAK_TYPE_MEMPAK:
				dma[offset + 5] = (byte) 1;
				break;
			case PAK_TYPE_RAW:
				dma[offset + 5] = (byte) 1;
				break;
			default:
				dma[offset + 5] = (byte) 0;
				break;
			}
			break;
		case 0x01: // read controller
			break;
		case 0x02: // read from controller pack
			address = (((dma[offset + 3] & 0xFF) << 8) | (dma[offset + 4] & 0xFF));
			switch (pakType)
			{
			case PAK_TYPE_MEMPAK:
			{
				expansionslot.write32bit(PAK_CONTROL_REG, pAddr);
				((BusDMA) expansionslot).readDMA(address, dmaObj, offset + 5, 0x20);
				dma[offset + 5 + 0x20] = mempacksCalulateCrc(dma, offset + 5);
				break;
			}
			case PAK_TYPE_RAW:
			{
				break;
			}
			default:
				Arrays.fill(dma, offset + 5, offset + 5 + 0x20, (byte) 0);
				dma[offset + 0x25] = (byte) 0;
			}
			break;
		case 0x03: // write controller pak
			address = (((dma[offset + 3] & 0xFF) << 8) | (dma[offset + 4] & 0xFF));
			switch (pakType)
			{
			case PAK_TYPE_MEMPAK:
			{
				expansionslot.write32bit(PAK_CONTROL_REG, pAddr);
				((BusDMA) expansionslot).writeDMA(address, dmaObj, offset + 5, 0x20);
				dma[offset + 5 + 0x20] = mempacksCalulateCrc(dma, offset + 5);
				break;
			}
			case PAK_TYPE_RAW:
			{
				break;
			}
			default:
				dma[offset + 5 + 0x25] = mempacksCalulateCrc(dma, offset + 5);
			}
			break;
		default:
			System.err.printf("Unknown ControllerCommand %d\n", dma[offset + 2]);
		}
	}

	// Private Methods /////////////////////////////////////////////////////////

	private int setYAxis(int b)
	{
		return ((b << 8) & 0xFF00);
	}

	private int setXAxis(int b)
	{
		return ((b) & 0x00FF);
	}

	private byte mempacksCalulateCrc(byte[] data, int offset)
	{
		int xorTap;
		byte crc = 0;
		int dataToCrc = 0;

		for (int count = 0; count < 0x21; count++)
		{
			for (int length = 0x80; length >= 1; length >>= 1)
			{
				xorTap = ((crc & 0x80) != 0) ? 0x85 : 0;
				crc <<= 1;
				if (count == 0x20)
				{
					crc &= 0xFF;
				}
				else
				{
					if ((data[offset + dataToCrc] & length) != 0)
					{
						crc |= 1;
					}
				}
				crc ^= xorTap;
			}
			dataToCrc++;
		}

		return crc;
	}

	private void config(int value)
	{
		switch (value)
		{
		case 0:
			try
			{
				Controllers.create();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			int count = Controllers.getControllerCount();
			for (int i = 0; i < count; i++)
			{
				controller = Controllers.getController(i);
				if (controller.getAxisCount() == 4 && controller.getButtonCount() >= 8)
				{
					buttonCount = controller.getButtonCount();
					break;
				}
			}

			keys[0] = new Key(setXAxis(80) | U_DPAD_VALUE, U_DPAD);
			keys[1] = new Key(setXAxis(-80) | D_DPAD_VALUE, D_DPAD);
			keys[2] = new Key(setYAxis(-80) | L_DPAD_VALUE, L_DPAD);
			keys[3] = new Key(setYAxis(80) | R_DPAD_VALUE, R_DPAD);
			keys[4] = new Key(START_BUTTON_VALUE, START_BUTTON);
			keys[5] = new Key(A_BUTTON_VALUE, A_BUTTON);
			keys[6] = new Key(B_BUTTON_VALUE, B_BUTTON);
			keys[7] = new Key(L_TRIG_VALUE, L_TRIG);
			keys[8] = new Key(R_TRIG_VALUE, R_TRIG);
			keys[9] = new Key(Z_TRIG_VALUE, Z_TRIG);
			keys[10] = new Key(U_CBUTTON_VALUE, U_CBUTTON);
			keys[11] = new Key(D_CBUTTON_VALUE, D_CBUTTON);
			keys[12] = new Key(L_CBUTTON_VALUE, L_CBUTTON);
			keys[13] = new Key(R_CBUTTON_VALUE, R_CBUTTON);

			executor.execute(poll);
			break;
		}
		config = true;
	}
}
