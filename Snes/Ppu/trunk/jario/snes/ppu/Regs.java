package jario.snes.ppu;

public class Regs
{
	public int ppu1_mdr;
	public int ppu2_mdr;

	public int vram_readbuffer;
	public int oam_latchdata;
	public int cgram_latchdata;
	public int bgofs_latchdata;
	public int mode7_latchdata;
	public boolean counters_latched;
	public boolean latch_hcounter;
	public boolean latch_vcounter;

	public int oam_iaddr;
	public int cgram_iaddr;

	// $2100 INIDISP
	public boolean display_disable;
	public int display_brightness;

	// $2102 OAMADDL
	// $2103 OAMADDH
	public int oam_baseaddr;
	public int oam_addr;
	public boolean oam_priority;

	// $2105 BGMODE
	public boolean bg3_priority;
	public int bgmode;

	// $210d BG1HOFS
	public int mode7_hoffset;

	// $210e BG1VOFS
	public int mode7_voffset;

	// $2115 VMAIN
	public boolean vram_incmode;
	public int vram_mapping;
	public int vram_incsize;

	// $2116 VMADDL
	// $2117 VMADDH
	public int vram_addr;

	// $211a M7SEL
	public int mode7_repeat;
	public boolean mode7_vflip;
	public boolean mode7_hflip;

	// $211b M7A
	public int m7a;

	// $211c M7B
	public int m7b;

	// $211d M7C
	public int m7c;

	// $211e M7D
	public int m7d;

	// $211f M7X
	public int m7x;

	// $2120 M7Y
	public int m7y;

	// $2121 CGADD
	public int cgram_addr;

	// $2133 SETINI
	public boolean mode7_extbg;
	public boolean pseudo_hires;
	public boolean overscan;
	public boolean interlace;

	// $213c OPHCT
	public int hcounter;

	// $213d OPVCT
	public int vcounter;
}
