/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.cartridge;

import jario.hardware.Bus8bit;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Cartridge implements Hardware, Bus8bit, Configurable
{
	class Page
	{
		public Bus8bit access = memory_unmapped;
		public int offset;
	}

	class UnmappedMemory implements Bus8bit
	{
		@Override
		public byte read8bit(int addr)
		{
			return 0; // (byte)CPU.cpu.regs.mdr;
		}

		@Override
		public void write8bit(int addr, byte data)
		{
		}
	}

	UnmappedMemory memory_unmapped = new UnmappedMemory();

	public enum Mode
	{
		Normal, BsxSlotted, Bsx, SufamiTurbo, SuperGameBoy
	}

	public enum Region
	{
		NTSC, PAL
	}

	public enum MapMode
	{
		Direct, Linear, Shadow
	}

	// assigned externally to point to file-system datafiles (msu1 and serial)
	// example: "/path/to/filename.sfc" would set this to "/path/to/filename"
	public String basename;
	private String cartridgeName;
	private boolean loaded;
	private Region region;
	private int ram_size;
	private Collection<Mapping> mapping = new ArrayList<Mapping>();
	private Page[] page = new Page[65536];
	private MappedRAM cartrom;
	private MappedRAM cartram;

	public Cartridge()
	{
		cartrom = new MappedRAM();
		cartram = new MappedRAM();

		for (int i = 0; i < page.length; i++) page[i] = new Page();
		loaded = false;
		reset();
	}

	@Override
	public void connect(int port, Hardware hw)
	{
	}

	@Override
	public void reset()
	{
		save();
		unload();
	}

	@Override
	public byte read8bit(int addr)
	{
		Page p = page[(addr & 0x00FFFFFF) >> 8];
		return p.access.read8bit(p.offset + (addr & 0x00FFFFFF));
	}

	@Override
	public void write8bit(int addr, byte data)
	{
		Page p = page[(addr & 0x00FFFFFF) >> 8];
		p.access.write8bit(p.offset + (addr & 0x00FFFFFF), data);
	}

	@Override
	public Object readConfig(String key)
	{
		switch (key)
		{
		case "region":
			return region.name().toLowerCase();
		}
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		switch (key)
		{
		case "romfile":
			cartridgeName = value.toString();
			loadDataFromRomFile();
			break;
		}
	}

	private void load(Mode cartridge_mode, String[] xml_list)
	{
		region = Region.NTSC;
		ram_size = 0;

		parse_xml(xml_list);

		if (ram_size > 0)
		{
			byte[] repeat = new byte[ram_size];
			Arrays.fill(repeat, (byte) 0xff);
			cartram.map(repeat, ram_size);
		}

		cartrom.write_protect(true);
		cartram.write_protect(false);

		if (!loaded)
		{
			map_reset();
			map_xml();
		}
		loaded = true;
	}

	private void unload()
	{
		cartridgeName = null;
		cartrom.reset();
		cartram.reset();

		if (loaded == false) { return; }
		loaded = false;
	}

	private void loadNormal(byte[] rom_data)
	{
		if (rom_data != null)
		{
			cartrom.copy(rom_data, rom_data.length);
		}
		String xmlrom = new SnesInformation(rom_data, rom_data.length).xml_memory_map;
		System.out.println(xmlrom);
		load(Cartridge.Mode.Normal, new String[] { xmlrom });
	}

	private void loadDataFromRomFile()
	{
		try
		{
			RandomAccessFile fs = new RandomAccessFile(new File(cartridgeName), "r");
			byte[] rom = new byte[(int) fs.length()];
			if (rom.length % 1024 != 0)
			{
				fs.skipBytes(0x200);
			}
			fs.read(rom);
			fs.close();
			loadNormal(rom);
			File save = new File("save" + File.separator + cartridgeName.substring(cartridgeName.lastIndexOf(File.separator) + 1, cartridgeName.lastIndexOf(".")) + ".save");
			if (save.exists())
			{
				fs = new RandomAccessFile(save, "r");
				byte[] ram = new byte[(int) fs.length()];
				fs.read(ram);
				fs.close();
				cartram.copy(ram, ram.length);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void save()
	{
		if (cartridgeName != null)
		{
			File save = new File("save" + File.separator + cartridgeName.substring(cartridgeName.lastIndexOf(File.separator) + 1, cartridgeName.lastIndexOf(".")) + ".save");
			try
			{
				RandomAccessFile fs = new RandomAccessFile(save, "rw");
				byte[] ram = cartram.data();
				if (ram != null) fs.write(ram);
				fs.close();
				System.out.println("saved: " + save.getAbsolutePath());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void map_reset()
	{
		for (int i = 0; i < page.length; i++)
		{
			page[i].access = memory_unmapped;
			page[i].offset = 0;
		}
	}

	private void map_xml()
	{
		for (Mapping m : mapping)
		{
			if (m.memory != null)
			{
				map(m.mode, m.banklo & 0xFF, m.bankhi & 0xFF, m.addrlo & 0xFFFF, m.addrhi & 0xFFFF, m.memory, m.offset, m.size);
			}
		}
	}

	private void parse_xml(String[] list)
	{
		mapping.clear();
		parse_xml_cartridge(list[0]);
	}

	private void parse_xml_cartridge(String data)
	{
		// System.out.print(data);

		Document document;
		try
		{
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document = db.parse(new InputSource(new ByteArrayInputStream(data.getBytes("utf-8"))));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
		if (!document.hasChildNodes()) { return; }

		if (document.getElementsByTagName("cartridge").getLength() != 0)
		{
			if (((Element) document.getElementsByTagName("cartridge").item(0)).hasAttribute("region"))
			{
				region = (((Element) document.getElementsByTagName("cartridge").item(0)).getAttribute("region").equals("NTSC")) ? Region.NTSC : Region.PAL;
			}
		}

		NodeList nl;
		nl = document.getElementsByTagName("rom");
		for (int i = 0; i < nl.getLength(); i++)
		{
			Element node = (Element) nl.item(i);
			xml_parse_rom(node);
		}
		nl = document.getElementsByTagName("ram");
		for (int i = 0; i < nl.getLength(); i++)
		{
			Element node = (Element) nl.item(i);
			xml_parse_ram(node);
		}
	}

	private void xml_parse_rom(Element root)
	{
		NodeList nl = root.getElementsByTagName("map");
		for (int i = 0; i < nl.getLength(); i++)
		{
			Element leaf = (Element) nl.item(i);
			Mapping m = new Mapping(cartrom);
			if (leaf.hasAttribute("address"))
			{
				xml_parse_address(m, leaf.getAttribute("address"));
			}
			if (leaf.hasAttribute("mode"))
			{
				xml_parse_mode(m, leaf.getAttribute("mode"));
			}
			if (leaf.hasAttribute("offset"))
			{
				m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
			}
			if (leaf.hasAttribute("size"))
			{
				m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
			}
			mapping.add(m);
		}
	}

	private void xml_parse_ram(Element root)
	{
		if (root.hasAttribute("size"))
		{
			ram_size = Integer.parseInt(root.getAttribute("size"), 16);
		}

		NodeList nl = root.getElementsByTagName("map");
		for (int i = 0; i < nl.getLength(); i++)
		{
			Element leaf = (Element) nl.item(i);
			Mapping m = new Mapping(cartram);
			if (leaf.hasAttribute("address"))
			{
				xml_parse_address(m, leaf.getAttribute("address"));
			}
			if (leaf.hasAttribute("mode"))
			{
				xml_parse_mode(m, leaf.getAttribute("mode"));
			}
			if (leaf.hasAttribute("offset"))
			{
				m.offset = Integer.parseInt(leaf.getAttribute("offset"), 16);
			}
			if (leaf.hasAttribute("size"))
			{
				m.size = Integer.parseInt(leaf.getAttribute("size"), 16);
			}
			mapping.add(m);
		}
	}

	private void xml_parse_address(Mapping m, String data)
	{
		String[] part = data.split(":");

		if (part.length != 2) { return; }

		String[] subpart = part[0].split("-");
		if (subpart.length == 1)
		{
			m.banklo = Integer.parseInt(subpart[0], 16);
			m.bankhi = m.banklo;
		}
		else if (subpart.length == 2)
		{
			m.banklo = Integer.parseInt(subpart[0], 16);
			m.bankhi = Integer.parseInt(subpart[1], 16);
		}

		subpart = part[1].split("-");
		if (subpart.length == 1)
		{
			m.addrlo = Integer.parseInt(subpart[0], 16);
			m.addrhi = m.addrlo;
		}
		else if (subpart.length == 2)
		{
			m.addrlo = Integer.parseInt(subpart[0], 16);
			m.addrhi = Integer.parseInt(subpart[1], 16);
		}
	}

	private void xml_parse_mode(Mapping m, String data)
	{
		if (data.equals("direct"))
		{
			m.mode = MapMode.Direct;
		}
		else if (data.equals("linear"))
		{
			m.mode = MapMode.Linear;
		}
		else if (data.equals("shadow"))
		{
			m.mode = MapMode.Shadow;
		}
	}

	private int mirror(int addr, int size)
	{
		int base_ = 0;
		if (size != 0)
		{
			int mask = 1 << 23;
			while (addr >= size)
			{
				while ((addr & mask) == 0)
				{
					mask >>= 1;
				}
				addr -= mask;
				if (size > mask)
				{
					size -= mask;
					base_ += mask;
				}
				mask >>= 1;
			}
			base_ += addr;
		}
		return base_;
	}

	private void map(int addr, Bus8bit access, int offset)
	{
		Page p = page[addr >> 8];
		p.access = access;
		p.offset = offset - addr;
	}

	private void map(MapMode mode, int bank_lo, int bank_hi, int addr_lo, int addr_hi, MappedRAM access, int offset, int size)
	{
		assert bank_lo <= bank_hi;
		assert addr_lo <= addr_hi;

		if (access.size() == -1) { return; }

		int page_lo = (addr_lo >> 8) & 0xFF;
		int page_hi = (addr_hi >> 8) & 0xFF;
		int index = 0;

		switch (mode)
		{
		case Direct:
		{
			for (int bank = bank_lo; bank <= bank_hi; bank++)
			{
				for (int page = page_lo; page <= page_hi; page++)
				{
					map((bank << 16) + (page << 8), access, (bank << 16) + (page << 8));
				}
			}
		}
			break;
		case Linear:
		{
			for (int bank = bank_lo; bank <= bank_hi; bank++)
			{
				for (int page = page_lo; page <= page_hi; page++)
				{
					map((bank << 16) + (page << 8), access, mirror(offset + index, access.size()));
					index += 256;
					if (size != 0)
					{
						index %= size;
					}
				}
			}
		}
			break;
		case Shadow:
		{
			for (int bank = bank_lo; bank <= bank_hi; bank++)
			{
				index += (page_lo * 256);
				if (size != 0)
				{
					index %= size;
				}

				for (int page = page_lo; page <= page_hi; page++)
				{
					map((bank << 16) + (page << 8), access, mirror(offset + index, access.size()));
					index += 256;
					if (size != 0)
					{
						index %= size;
					}
				}

				index += ((255 - page_hi) * 256);
				if (size != 0)
				{
					index %= size;
				}
			}
		}
			break;
		}
	}
}
