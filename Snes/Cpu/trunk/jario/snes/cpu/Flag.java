package jario.snes.cpu;

public class Flag
{
	public boolean n, v, m, x, d, i, z, c;

    public int get()
    {
        return (((n ? 1 : 0) << 7) + ((v ? 1 : 0) << 6) + ((m ? 1 : 0) << 5) + ((x ? 1 : 0) << 4)
             + ((d ? 1 : 0) << 3) + ((i ? 1 : 0) << 2) + ((z ? 1 : 0) << 1) + ((c ? 1 : 0) << 0));
    }

    public void set(int data)
    {
        n = (data & 0x80) != 0;
        v = (data & 0x40) != 0;
        m = (data & 0x20) != 0;
        x = (data & 0x10) != 0;
        d = (data & 0x08) != 0;
        i = (data & 0x04) != 0;
        z = (data & 0x02) != 0;
        c = (data & 0x01) != 0;
    }

    public Flag()
    {
        n = v = m = x = d = i = z = c = false;
    }
}
