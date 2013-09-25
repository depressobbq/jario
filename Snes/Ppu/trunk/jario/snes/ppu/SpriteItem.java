package jario.snes.ppu;

public class SpriteItem
{
	public int x;
    public int y;
    public int character;
    public boolean nameselect;
    public boolean vflip;
    public boolean hflip;
    public int priority;
    public int palette;
    public boolean size;

    private static int[] Width1 = { 8, 8, 8, 16, 16, 32, 16, 16 };
    private static int[] Width2 = { 16, 32, 64, 32, 64, 64, 32, 32 };
    private static int[] Height1 = { 8, 8, 8, 16, 16, 32, 32, 32 };
    private static int[] Height2 = { 16, 32, 64, 32, 64, 64, 64, 32 };

    public int width(int base_size)
    {
        if (size == false)
        {
            return Width1[base_size];
        }
        else
        {
            return Width2[base_size];
        }
    }

    public int height(int base_size, boolean interlace)
    {
        if (size == false)
        {
            if (interlace && base_size >= 6)
            {
                return 16;
            }
            return Height1[base_size];
        }
        else
        {
            return Height2[base_size];
        }
    }
}
