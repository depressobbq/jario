/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.system;

import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.awt.Canvas;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

public class SnesSystem implements Hardware
{
	private Canvas canvas;
	private JFrame window;
	private Hardware video;
	private Hardware audio;
	private Hardware controller;
	private Map<String, Hardware> controllers = new HashMap<String, Hardware>();
	private Hardware console;
	private Hardware cartridge;

	@SuppressWarnings("serial")
	private class Jario64MenuBar extends JMenuBar
	{
		public Jario64MenuBar()
		{
			add(makeFileMenu());
			add(makeControllerMenu());
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
					// choose a rom file
					File romFile = null;
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setDialogTitle("Open ROM");
					fileChooser.setFileFilter(new FileFilter()
					{
						@Override
						public boolean accept(File f)
						{
							return f.getName().endsWith(".smc") || f.getName().endsWith(".sfc");
						}

						@Override
						public String getDescription()
						{
							return "ROM files (.smc, .sfc)";
						}
					});
					int returnVal = fileChooser.showOpenDialog(null);
					if (returnVal == JFileChooser.APPROVE_OPTION)
					{
						romFile = fileChooser.getSelectedFile();
						LoadCartridge(romFile.getAbsolutePath());
					}
					else
					{
						return;
					}
				}
			});
			fileMenu.add(loadRom);

			JMenuItem exit = new JMenuItem("Exit");
			exit.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					exit();
				}
			});
			fileMenu.add(exit);

			return fileMenu;
		}

		private JMenu makeSettingsMenu()
		{
			JMenu settingsMenu = new JMenu();
			settingsMenu.setText("Settings");

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

			JCheckBoxMenuItem fps30 = new JCheckBoxMenuItem("30fps");
			fps30.setState((Integer) ((Configurable) console).readConfig("fps") == 30);
			fps30.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					JCheckBoxMenuItem i = (JCheckBoxMenuItem) evt.getSource();
					if (i.isSelected())
					{
						((Configurable) console).writeConfig("fps", 30);
						((Configurable) audio).writeConfig("samplerate", 16160);
					}
					else
					{
						((Configurable) console).writeConfig("fps", 60);
						((Configurable) audio).writeConfig("samplerate", 33334);
					}
				}
			});
			settingsMenu.add(fps30);

			return settingsMenu;
		}
		
		private JMenu makeControllerMenu()
		{
			JMenu controllerMenu = new JMenu();
			controllerMenu.setText("Controller");
			
			ButtonGroup group = new ButtonGroup();
			
			for (Hardware hardware : controllers.values())
			{
				JRadioButtonMenuItem controllerOption = new JRadioButtonMenuItem(hardware.getClass().getSimpleName());
				if (controller != null && hardware.getClass().getName().equals(controller.getClass().getName()))
				{
					controllerOption.setSelected(true);
				}
				controllerOption.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt)
					{
						JRadioButtonMenuItem i = (JRadioButtonMenuItem) evt.getSource();
						if (i.isSelected())
						{
							controller = controllers.get(i.getText());
							console.connect(0, controller); // controller1
						}
					}
				});
				group.add(controllerOption);
				controllerMenu.add(controllerOption);
			}

			return controllerMenu;
		}
	}

	private WindowAdapter winListener = new WindowAdapter()
	{
		public void windowClosing(WindowEvent e)
		{
			exit();
		}
	};

	public SnesSystem()
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
			
			ServiceLoader<Hardware> sl = ServiceLoader.load(Hardware.class, loader);
			Iterator<Hardware> it = sl.iterator();
			while (it.hasNext())
			{
				Hardware hardware = it.next();
				controllers.put(hardware.getClass().getSimpleName(), hardware);
				if (prop.getProperty("CONTROLLER", "CONTROLLER").equals(hardware.getClass().getName()))
				{
					controller = hardware;
				}
			}

			video = (Hardware) Class.forName(prop.getProperty("VIDEO_PLAYER", "VIDEO_PLAYER"), true, loader).newInstance();
			audio = (Hardware) Class.forName(prop.getProperty("AUDIO_PLAYER", "AUDIO_PLAYER"), true, loader).newInstance();
			if (controller == null) controller = (Hardware) Class.forName(prop.getProperty("CONTROLLER", "CONTROLLER"), true, loader).newInstance();
			console = (Hardware) Class.forName(prop.getProperty("CONSOLE", "CONSOLE"), true, loader).newInstance();
			cartridge = (Hardware) Class.forName(prop.getProperty("CARTRIDGE", "CARTRIDGE"), true, loader).newInstance();
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		window = new JFrame("Super Jario ES");
		window.addWindowListener(winListener);
		window.setJMenuBar(new Jario64MenuBar());
		canvas = new Canvas();
		canvas.setPreferredSize(new java.awt.Dimension(512, 448));
		window.getContentPane().add(canvas);
		window.pack();
		window.setVisible(true);

		((Configurable) video).writeConfig("window", window);

		console.connect(3, video);
		console.connect(4, audio);
		console.connect(0, controller); // controller1
		console.connect(1, null); // controller2
	}

	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
	}

	private void LoadCartridge(String cartridgeName)
	{
		console.reset();
		cartridge.reset();
		((Configurable) cartridge).writeConfig("romfile", cartridgeName);
		System.out.println("opened: " + cartridgeName);
		console.connect(2, cartridge);
		((Clockable) console).clock(1L); // starts cpu thread
	}

	private void exit()
	{
		// remove cartridge
		console.connect(2, null);
		console.reset();
		if (cartridge != null)
		{
			// save cartridge data
			cartridge.reset();
		}
		System.exit(0);
	}
}
