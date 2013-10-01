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

package jario.n64.system;

import jario.hardware.Bus32bit;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class Jario64System implements Hardware
{
	private static final String AUTO_SAVE_DIR = "./save/";

	private static final int CONSOLE_CONTROLLER1_PORT = 1;
	private static final int CONSOLE_CONTROLLER2_PORT = 2;
	private static final int CONSOLE_CONTROLLER3_PORT = 3;
	private static final int CONSOLE_CONTROLLER4_PORT = 4;
	private static final int CONSOLE_CARTRIDGE_PORT = 5;
	private static final int CONSOLE_VIDEO_PORT = 6;
	private static final int CONSOLE_AUDIO_PORT = 7;

	private static final int CONTROLLER_EXPANSION_PORT = 0;

	private static final int VIDEO_ENABLE_REG = 1;

	private ExecutorService romThreadExecutor;

	private Hardware console;
	private Hardware mempak;
	private Hardware controller1;
	private Hardware controller2;
	private Hardware controller3;
	private Hardware controller4;
	private Bus32bit video;
	private Hardware audio;
	private Hardware cartridge;

	private WindowAdapter winListener = new WindowAdapter()
	{
		public void windowClosing(WindowEvent e)
		{
			exit("Window Closed.");
		}
	};

	private final Runnable romProcessThread = new Runnable()
	{
		public void run()
		{
			insertGameCartridge();
		}
	};

	@SuppressWarnings("serial")
	private class Jario64MenuBar extends JMenuBar
	{
		public Jario64MenuBar()
		{
			add(makeFileMenu());
			add(makeSettingsMenu());
		}

		private JMenu makeFileMenu()
		{
			JMenu fileMenu = new JMenu();
			fileMenu.setText("File");

			JMenuItem loadRom = new JMenuItem("Insert Game Cartridge");
			loadRom.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					// first remove any previously inserted cartridges
					removeGameCartridge();
					romThreadExecutor.execute(romProcessThread);
				}
			});
			fileMenu.add(loadRom);

			JMenuItem unloadRom = new JMenuItem("Remove Game Cartridge");
			unloadRom.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					removeGameCartridge();
				}
			});
			fileMenu.add(unloadRom);

			JMenuItem exit = new JMenuItem("Exit");
			exit.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					exit("Goodbye");
				}
			});
			fileMenu.add(exit);

			return fileMenu;
		}

		private JMenu makeSettingsMenu()
		{
			JMenu settingsMenu = new JMenu();
			settingsMenu.setText("Settings");

			JCheckBoxMenuItem cache = new JCheckBoxMenuItem("Cache Instructions");
			cache.setState((Boolean) ((Configurable) console).readConfig("instructioncache"));
			cache.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JCheckBoxMenuItem i = (JCheckBoxMenuItem) evt.getSource();
					((Configurable) console).writeConfig("instructioncache", i.isSelected());
				}
			});
			settingsMenu.add(cache);

			JCheckBoxMenuItem audioToggle = new JCheckBoxMenuItem("Enable Audio");
			audioToggle.setState((Boolean) ((Configurable) audio).readConfig("enable"));
			audioToggle.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JCheckBoxMenuItem i = (JCheckBoxMenuItem) evt.getSource();
					((Configurable) audio).writeConfig("enable", i.isSelected());
				}
			});
			settingsMenu.add(audioToggle);

			JCheckBoxMenuItem frameLimit = new JCheckBoxMenuItem("Enable Frame Limit");
			frameLimit.setState((Boolean) ((Configurable) console).readConfig("framelimit"));
			frameLimit.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JCheckBoxMenuItem i = (JCheckBoxMenuItem) evt.getSource();
					((Configurable) console).writeConfig("framelimit", i.isSelected());
				}
			});
			settingsMenu.add(frameLimit);

			JCheckBoxMenuItem frameBuffer = new JCheckBoxMenuItem("Use Video Frame Buffer");
			frameBuffer.setState((Boolean) ((Configurable) console).readConfig("framebuffer"));
			frameBuffer.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JCheckBoxMenuItem i = (JCheckBoxMenuItem) evt.getSource();
					((Configurable) console).writeConfig("framebuffer", i.isSelected());
				}
			});
			settingsMenu.add(frameBuffer);

			return settingsMenu;
		}
	}

	public Jario64System()
	{
		try
		{
			File dir = new File("components" + File.separator);
			File file = new File("components.properties");
			ClassLoader loader = this.getClass().getClassLoader();
			Properties prop = new Properties();
			try
			{
				if (dir.exists() && dir.listFiles().length > 0)
				{
					File[] files = dir.listFiles();
					URL[] urls = new URL[files.length];
					for (int i = 0; i < files.length; i++) urls[i] = files[i].toURI().toURL();
					loader = new URLClassLoader(urls, this.getClass().getClassLoader());
				}
				URL url = file.exists() ? file.toURI().toURL() : loader.getResource("resources" + File.separator + "components.properties");
				if (url != null) prop.load(url.openStream());
			}
			catch (IOException e)
			{
			}

			cartridge = (Hardware) Class.forName(prop.getProperty("CARTRIDGE", "CARTRIDGE"), true, loader).newInstance();
			mempak = (Hardware) Class.forName(prop.getProperty("MEMPAK", "MEMPAK"), true, loader).newInstance();
			controller1 = (Hardware) Class.forName(prop.getProperty("CONTROLLER", "CONTROLLER"), true, loader).newInstance();
			controller2 = (Hardware) Class.forName(prop.getProperty("CONTROLLER", "CONTROLLER"), true, loader).newInstance();
			controller3 = (Hardware) Class.forName(prop.getProperty("CONTROLLER", "CONTROLLER"), true, loader).newInstance();
			controller4 = (Hardware) Class.forName(prop.getProperty("CONTROLLER", "CONTROLLER"), true, loader).newInstance();
			console = (Hardware) Class.forName(prop.getProperty("CONSOLE", "CONSOLE"), true, loader).newInstance();
			audio = (Hardware) Class.forName(prop.getProperty("AUDIO_PLAYER", "AUDIO_PLAYER"), true, loader).newInstance();
			video = (Bus32bit) Class.forName(prop.getProperty("VIDEO_PLAYER", "VIDEO_PLAYER"), true, loader).newInstance();
		}
		catch (Exception e)
		{
			System.err.println("Missing resources.");
			e.printStackTrace();
			return;
		}

		// connect ui to display
		((Configurable) video).writeConfig("callback", winListener);
		((Configurable) video).writeConfig("menu", new Jario64MenuBar());

		romThreadExecutor = Executors.newSingleThreadExecutor();

		// connect a/v to console
		console.connect(CONSOLE_VIDEO_PORT, (Hardware) video);
		console.connect(CONSOLE_AUDIO_PORT, audio);

		// Connect mempak to controller
		controller1.connect(CONTROLLER_EXPANSION_PORT, mempak);

		// Connect controllers to console
		console.connect(CONSOLE_CONTROLLER1_PORT, controller1);
		console.connect(CONSOLE_CONTROLLER2_PORT, controller2);
		console.connect(CONSOLE_CONTROLLER3_PORT, controller3);
		console.connect(CONSOLE_CONTROLLER4_PORT, controller4);
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

	// Private Methods /////////////////////////////////////////////////////////

	private void exit(String msg)
	{
		// Unplug controller
		// console.connectBus(CONSOLE_CONTROLLER0_PORT, null);
		// console.connectBus(CONSOLE_CONTROLLER1_PORT, null);
		// Unplug cartridge
		// console.connectBus(CONSOLE_CARTRIDGE_PORT, null);
		System.exit(0);
	}

	private void removeGameCartridge()
	{
		// flush any save files to disk
		mempak.reset();
		// remove the game cartridge
		console.connect(CONSOLE_CARTRIDGE_PORT, null);
		// clear the display
		video.write32bit(VIDEO_ENABLE_REG, 0);
	}

	private void insertGameCartridge()
	{
		// clear the display
		video.write32bit(VIDEO_ENABLE_REG, 0);

		// choose a rom file
		File romFile = null;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Open ROM");
		int returnVal = fileChooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
			romFile = fileChooser.getSelectedFile();
		else
			return;

		// create save directory
		File Directory = new File(AUTO_SAVE_DIR);
		if (!Directory.exists())
			Directory.mkdirs();
		String romName = romFile.getName().substring(0, romFile.getName().lastIndexOf('.'));
		String path = Directory.getAbsolutePath() + "/" + romName;

		// setup file links
		((Configurable) mempak).writeConfig("file", path + ".mpk");
		setSaveFile(path);
		loadCartRom(romFile.getAbsolutePath());

		// enable the display
		video.write32bit(VIDEO_ENABLE_REG, 1);

		// insert the game cartridge
		console.connect(CONSOLE_CARTRIDGE_PORT, cartridge);
	}

	private void loadCartRom(String fileName)
	{
		((Configurable) cartridge).writeConfig("romfile", fileName);
		System.out.printf("Opened rom: %s\n", fileName);
	}

	private void setSaveFile(String fileName)
	{
		((Configurable) cartridge).writeConfig("savefile", fileName);
	}
}
