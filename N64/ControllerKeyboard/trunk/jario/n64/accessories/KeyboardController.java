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

package jario.n64.accessories;

import jario.hardware.Bus32bit;
import jario.hardware.BusDMA;
import jario.hardware.Hardware;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class KeyboardController implements Hardware, BusDMA
{
	private static final int PAK_TYPE_NONE = 1;
	private static final int PAK_TYPE_MEMPAK = 2;
	// private static final int PAK_TYPE_RUMBLE_PAK = 3; // not implemeted for non raw data
	// private static final int PAK_TYPE_TANSFER_PAK = 4; // not implemeted for non raw data
	private static final int PAK_TYPE_RAW = 5; // the controller plugin is passed in raw data

	private static final int PAK_TYPE_REG = 0;
	private static final int PAK_CONTROL_REG = 1;

	private static final int NUMBER_OF_BUTTONS = 14;

	private static final int U_DPAD = 0;
	private static final int D_DPAD = 1;
	private static final int L_DPAD = 2;
	private static final int R_DPAD = 3;
	private static final int START_BUTTON = 4;
	private static final int A_BUTTON = 5;
	private static final int B_BUTTON = 6;
	private static final int L_TRIG = 7;
	private static final int R_TRIG = 8;
	private static final int Z_TRIG = 9;
	private static final int U_CBUTTON = 10;
	private static final int D_CBUTTON = 11;
	private static final int L_CBUTTON = 12;
	private static final int R_CBUTTON = 13;

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

		public Key(int value)
		{
			this.value = value;
		}
	};

	private int controllerButtons;
	private Key[] keys = new Key[NUMBER_OF_BUTTONS];
	private int pakType;
	private boolean config;

	private Bus32bit expansionslot;

	private class ControllerListener implements AWTEventListener
	{
		public void eventDispatched(AWTEvent event)
		{
			if (!config)
				return;
			KeyEvent kevt = (KeyEvent) event;
			if (kevt.getID() == KeyEvent.KEY_PRESSED)
			{
				for (int count = 0; count < NUMBER_OF_BUTTONS; count++)
				{
					if (keys[count].vkey == kevt.getKeyCode())
					{
						controllerButtons |= keys[count].value;
						return;
					}
				}
			}
			else if (kevt.getID() == KeyEvent.KEY_RELEASED)
			{
				for (int count = 0; count < NUMBER_OF_BUTTONS; count++)
				{
					if (keys[count].vkey == kevt.getKeyCode())
					{
						controllerButtons &= ~keys[count].value;
						return;
					}
				}
			}
		}
	};

	public KeyboardController()
	{
		Toolkit.getDefaultToolkit().addAWTEventListener(new ControllerListener(), AWTEvent.KEY_EVENT_MASK);

		pakType = PAK_TYPE_NONE;

		keys[U_DPAD] = new Key(setXAxis(80) | U_DPAD_VALUE);
		keys[D_DPAD] = new Key(setXAxis(-80) | D_DPAD_VALUE);
		keys[L_DPAD] = new Key(setYAxis(-80) | L_DPAD_VALUE);
		keys[R_DPAD] = new Key(setYAxis(80) | R_DPAD_VALUE);
		keys[START_BUTTON] = new Key(START_BUTTON_VALUE);
		keys[A_BUTTON] = new Key(A_BUTTON_VALUE);
		keys[B_BUTTON] = new Key(B_BUTTON_VALUE);
		keys[L_TRIG] = new Key(L_TRIG_VALUE);
		keys[R_TRIG] = new Key(R_TRIG_VALUE);
		keys[Z_TRIG] = new Key(Z_TRIG_VALUE);
		keys[U_CBUTTON] = new Key(U_CBUTTON_VALUE);
		keys[D_CBUTTON] = new Key(D_CBUTTON_VALUE);
		keys[L_CBUTTON] = new Key(L_CBUTTON_VALUE);
		keys[R_CBUTTON] = new Key(R_CBUTTON_VALUE);
	}

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
			keys[U_DPAD].vkey = KeyEvent.VK_UP;
			keys[D_DPAD].vkey = KeyEvent.VK_DOWN;
			keys[L_DPAD].vkey = KeyEvent.VK_LEFT;
			keys[R_DPAD].vkey = KeyEvent.VK_RIGHT;
			keys[START_BUTTON].vkey = KeyEvent.VK_ENTER;
			keys[A_BUTTON].vkey = KeyEvent.VK_X;
			keys[B_BUTTON].vkey = KeyEvent.VK_C;
			keys[L_TRIG].vkey = KeyEvent.VK_A;
			keys[R_TRIG].vkey = KeyEvent.VK_S;
			keys[Z_TRIG].vkey = KeyEvent.VK_Z;
			keys[U_CBUTTON].vkey = KeyEvent.VK_HOME;
			keys[D_CBUTTON].vkey = KeyEvent.VK_END;
			keys[L_CBUTTON].vkey = KeyEvent.VK_DELETE;
			keys[R_CBUTTON].vkey = KeyEvent.VK_PAGE_DOWN;
			break;
		default:
			keys[U_DPAD].vkey = -1;
			keys[D_DPAD].vkey = -1;
			keys[L_DPAD].vkey = -1;
			keys[R_DPAD].vkey = -1;
			keys[START_BUTTON].vkey = -1;
			keys[A_BUTTON].vkey = -1;
			keys[B_BUTTON].vkey = -1;
			keys[L_TRIG].vkey = -1;
			keys[R_TRIG].vkey = -1;
			keys[Z_TRIG].vkey = -1;
			keys[U_CBUTTON].vkey = -1;
			keys[D_CBUTTON].vkey = -1;
			keys[L_CBUTTON].vkey = -1;
			keys[R_CBUTTON].vkey = -1;
			break;
		}
		config = true;
	}
}
