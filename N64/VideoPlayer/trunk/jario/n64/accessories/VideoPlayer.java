/**
 * Copyright 2009, 2013 Jason LaDere
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
 */

package jario.n64.accessories;

import jario.hardware.Bus32bit;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.awt.Component;
import java.awt.event.WindowListener;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class VideoPlayer implements Hardware, Bus32bit, Configurable
{
	private JFrame hMainWindow;
	private JPanel panel;
	private Component screen;

	public VideoPlayer()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		hMainWindow = new JFrame("Jario64");
		panel = new JPanel();
		panel.setPreferredSize(new java.awt.Dimension(640, 480));
		hMainWindow.getContentPane().add(panel);
		hMainWindow.pack();
		hMainWindow.setVisible(true);
	}

	@Override
	public void connect(int port, Hardware bus)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void reset()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int read32bit(int pAddr)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void write32bit(int reg, int value)
	{
		switch (reg)
		{
		case 0: // fps
			hMainWindow.setTitle(String.format("FPS: %d", value));
			break;
		case 1: // video signal?
			if (value != 0)
			{
				if (screen != null)
				{
					screen.setIgnoreRepaint(true);
				}
			}
			else
			{
				if (screen != null)
				{
					screen.setIgnoreRepaint(false);
					screen.getGraphics().clearRect(0, 0, screen.getWidth(), screen.getHeight());
				}
				hMainWindow.setTitle("Jario64");
			}
			break;
		}
	}

	@Override
	public Object readConfig(String key)
	{
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		if (key.equals("screen"))
		{
			screen = (Component) value;
			hMainWindow.getContentPane().removeAll();
			hMainWindow.getContentPane().add(screen);
			hMainWindow.pack();
			while (!screen.isDisplayable())
			{
				try
				{
					Thread.sleep(50);
				}
				catch (InterruptedException e)
				{
				}
			}
		}
		else if (key.equals("callback"))
		{
			hMainWindow.addWindowListener((WindowListener) value);
		}
		else if (key.equals("menu"))
		{
			hMainWindow.setJMenuBar((JMenuBar) value);
		}
	}
}
