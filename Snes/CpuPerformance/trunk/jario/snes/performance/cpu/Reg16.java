package jario.snes.performance.cpu;

public class Reg16
{
	private int d;

	public int get() { return d; }
	public void set(int i) { d = i & 0xFFFF; }

	public int w() { return d; }
	public void w(int i) { d = i & 0xFFFF; }

	public int l() { return d & 0xFF; }
	public void l(int i) { d = (d & 0xFF00) | (i & 0xFF); }

	public int h() { return (d >> 8) & 0xFF; }
	public void h(int i) { d = (d & 0x00FF) | ((i & 0xFF) << 8); }

	public Reg16()
	{
		d = 0;
	}
}
