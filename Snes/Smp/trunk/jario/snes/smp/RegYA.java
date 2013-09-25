package jario.snes.smp;

public class RegYA
{
	private int[] reg;
    private int hi_offset, lo_offset;

    public int get()
    {
        return (reg[hi_offset] << 8) + reg[lo_offset];
    }

    public void set(int data)
    {
        reg[hi_offset] = (data >> 8) & 0xFF;
        reg[lo_offset] = data & 0xFF;
    }

    public RegYA(int[] reg_, int hi_offset_, int lo_offset_)
    {
        reg = reg_;
        hi_offset = hi_offset_;
        lo_offset = lo_offset_;
    }
}
