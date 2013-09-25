package jario.snes.ppu;

import jario.hardware.Bus32bit;
import jario.hardware.Bus8bit;
import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;

public class PPUCounter implements Hardware, Clockable, Bus32bit, Configurable
{
	private Bus8bit display;
	private boolean pal;
	
	@Override
	public void connect(int port, Hardware hw)
	{
		switch (port)
		{
			case 0: display = (Bus8bit)hw; break;
		}
	}
    
	@Override
    public void clock(long clocks)
    {   // tick
		if (clocks == 0L)
		{
			status.hcounter += 2;  //increment by smallest unit of time
	        if (status.hcounter >= 1360 && status.hcounter == lineclocks())
	        {
	            status.hcounter = 0;
	            vcounter_tick();
	        }
	
	        history.index = (history.index + 1) & 2047;
	        history.field[history.index] = status.field;
	        history.vcounter[history.index] = status.vcounter;
	        history.hcounter[history.index] = status.hcounter;
		}
		else
		{
			tick((int)clocks);
		}
    }
	
	final void tick(int clocks)
    {
		status.hcounter += clocks;
        if (status.hcounter >= lineclocks())
        {
            status.hcounter -= lineclocks();
            vcounter_tick();
        }
    }
	
	@Override
	public final int read32bit(int address)
	{   // TODO: Do this a better way
		if (address >= 2048)
		{
			if (address >= 4096)
			{
				return history.hcounter[(history.index - ((address - 4096) >> 1)) & 2047];
			}
			else
			{
				return history.vcounter[(history.index - ((address - 2048) >> 1)) & 2047];
			}
		}
		switch (address)
		{
			case 0:	return status.vcounter;
			case 1:	return status.hcounter;
			case 2:	return status.field ? 1 : 0;
			case 3:	return hdot();
			case 4:	return lineclocks();
		}
		return 0;
	}

	@Override
	public void write32bit(int address, int data) {	}

    final int hdot()
    {
    	if (!pal && !status.interlace && status.vcounter == 240 && status.field)
        {
            return (status.hcounter >> 2);
        }
        else
        {
            return ((status.hcounter - (((status.hcounter > 1292) ? 1 : 0) << 1) - (((status.hcounter > 1310) ? 1 : 0) << 1)) >> 2);
        }
    }

    final int lineclocks()
    {
    	if (!pal && !status.interlace && status.vcounter == 240 && status.field)
        {
            return 1360;
        }
        return 1364;
    }

    @Override
    public void reset()
    {
        status.interlace = false;
        status.field = false;
        status.vcounter = 0;
        status.hcounter = 0;
        history.index = 0;

        for (int i = 0; i < 2048; i++)
        {
            history.field[i] = false;
            history.vcounter[i] = 0;
            history.hcounter[i] = 0;
        }
    }

    private Runnable Scanline = null;

    private void vcounter_tick()
    {
        if (++status.vcounter == 128)
        {
            status.interlace = display.read8bit(0) != 0;
        }

        if ((!pal && status.interlace == false && status.vcounter == 262)
                || (!pal && status.interlace == true && status.vcounter == 263)
                || (!pal && status.interlace == true && status.vcounter == 262 && status.field == true)
                || (pal && status.interlace == false && status.vcounter == 312)
                || (pal && status.interlace == true && status.vcounter == 313)
                || (pal && status.interlace == true && status.vcounter == 312 && status.field == true)
                )
        {
            status.vcounter = 0;
            status.field = !status.field;
        }
        if (Scanline != null)
        {
            Scanline.run();
        }
    }

    Status status = new Status();
    private History history = new History();

	@Override
	public Object readConfig(String key)
	{
		return null;
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		switch (key)
		{
			case "scanline":
				Scanline = (Runnable)value;
				break;
			case "region":
				pal = !value.toString().equals("ntsc");
				break;
		}
		
	}
}
