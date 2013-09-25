package jario.snes.console;

import jario.hardware.Clockable;
import jario.hardware.Configurable;
import jario.hardware.Hardware;
import jario.snes.configuration.Configuration;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Console implements Hardware, Clockable, Configurable
{
	ExecutorService executor;
	private boolean done;
	
	Hardware bus;
	Hardware cpu;
	Hardware ppu;
	Hardware dsp;
	Hardware smp;
	Hardware counter_cpu;
	Hardware counter_ppu;
	Hardware display;
	Hardware apuram;
	Hardware cartridge;
	Hardware video;
	Hardware audio;
	Hardware input;

    public enum Region { NTSC, PAL, Autodetect }
    //public enum ExpansionPortDevice : uint { None = 0, BSX = 1 }
    
    private Region region;
    
    public Console()
    {
        region = Region.Autodetect;
        //expansion = ExpansionPortDevice.None;
        executor = Executors.newSingleThreadExecutor();
        
        // coprocessors init
    	
 		try
 		{
 			File dir = new File("components"+File.separator);
     		File file = new File("components.properties");
     		ClassLoader loader = this.getClass().getClassLoader();
 			Properties prop = new Properties();
 			try
 			{
 				if (dir.exists() && dir.listFiles().length > 0)
 				{
 					File[] files = dir.listFiles(new FileFilter() { public boolean accept(File f) { return f.getPath().toLowerCase().endsWith(".jar"); } });
 					URL[] urls = new URL[files.length];
 					for (int i=0; i<files.length; i++) urls[i] = files[i].toURI().toURL();
 					loader = new URLClassLoader(urls, this.getClass().getClassLoader());
 				}
 				URL url = file.exists() ? file.toURI().toURL() : loader.getResource("resources"+File.separator+"components.properties");
 				if (url != null) prop.load(url.openStream());
 			}
 			catch (IOException e) { }
             
 			bus = (Hardware)Class.forName(prop.getProperty("MEMORY", "MEMORY"), true, loader).newInstance();
 			cpu = (Hardware)Class.forName(prop.getProperty("CPU", "CPU"), true, loader).newInstance();
 			ppu = (Hardware)Class.forName(prop.getProperty("PPU", "PPU"), true, loader).newInstance();
 			smp = (Hardware)Class.forName(prop.getProperty("SMP", "SMP"), true, loader).newInstance();
 			dsp = (Hardware)Class.forName(prop.getProperty("DSP", "DSP"), true, loader).newInstance();
 			apuram =(Hardware)Class.forName(prop.getProperty("APU_RAM", "APU_RAM"), true, loader).newInstance();
 			counter_cpu = (Hardware)Class.forName(prop.getProperty("PPU_COUNTER", "PPU_COUNTER"), true, loader).newInstance();
 			counter_ppu = (Hardware)Class.forName(prop.getProperty("PPU_COUNTER", "PPU_COUNTER"), true, loader).newInstance();
 			display = (Hardware)Class.forName(prop.getProperty("PPU_DISPLAY", "PPU_DISPLAY"), true, loader).newInstance();
 	    	video = (Hardware)Class.forName(prop.getProperty("DAC_VIDEO", "VIDEO"), true, loader).newInstance();
 	        audio = (Hardware)Class.forName(prop.getProperty("DAC_AUDIO", "AUDIO"), true, loader).newInstance();
 	        input = (Hardware)Class.forName(prop.getProperty("INPUT", "INPUT"), true, loader).newInstance();
 		}
 		catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
 		{
 			e.printStackTrace();
 		}
         
         // connections
 		bus.connect(0, cpu);
 		bus.connect(1, ppu);
 		((Configurable)bus).writeConfig("wram init value", (byte)Configuration.config.cpu.wram_init_value);
 		
 		cpu.connect(0, bus);
 		cpu.connect(1, smp);
 		cpu.connect(2, input);
 		cpu.connect(3, counter_cpu);
 		cpu.connect(4, video);
 		cpu.connect(5, display);
 		cpu.connect(6, ppu);
 		counter_cpu.connect(0, display);
 		((Configurable)cpu).writeConfig("cpu version", Configuration.config.cpu.version);
 		
 		ppu.connect(0, display);
 		ppu.connect(1, counter_ppu);
 		ppu.connect(2, counter_cpu);
 		display.connect(0, ppu);
 		counter_ppu.connect(0, display);
 		((Configurable)ppu).writeConfig("ppu1 version", Configuration.config.ppu1.version);
         ((Configurable)ppu).writeConfig("ppu2 version", Configuration.config.ppu2.version);
 		
 		smp.connect(0, apuram);
 		smp.connect(1, dsp);
 		smp.connect(2, cpu);
 		
 		dsp.connect(0, apuram);
 		dsp.connect(1, audio);
 		
 		video.connect(1, display);
 		video.connect(2, ppu);
 		video.connect(3, counter_ppu);
 		video.connect(4, audio); // DAC
 		
 		input.connect(2, display);
    }
    
    @Override
	public void connect(int port, Hardware hw)
    {
		switch (port)
		{
			case 0: input.connect(0, hw); break;
			case 1: input.connect(1, hw); break;
			case 2:
				cartridge = hw;
				bus.connect(2, cartridge);
				if (cartridge != null)
				{
					insertCartridge();
				}
				break;
			case 3: video.connect(0, hw); break;
			case 4: audio.connect(0, hw); break;
		}
	}

    @Override
    public void clock(long clocks)
    {
    	final Clockable cpu_clk = (Clockable)cpu;
    	done = false;
    	
    	executor.execute(new Runnable() {
    		public void run()
			{
				while (!done)
				{
	    			cpu_clk.clock(50000L);
				}
			}
		});
    }
    
    @Override
    public void reset()
    {
    	done = true;
    	
    	try
    	{
			Thread.sleep(1000);
		}
    	catch (InterruptedException e)
    	{
			e.printStackTrace();
		}
    	
        cpu.reset();
        smp.reset();
        dsp.reset();
        ppu.reset();
        
        audio.reset();
        video.reset();

        ((Clockable)input).clock(1L);
    }

	@Override
	public Object readConfig(String key)
	{
		switch (key)
		{
			case "accuracy": return ((Configurable)ppu).readConfig("accuracy");
			case "fps": return ((Configurable)video).readConfig("fps");
			default: return null;
		}
	}

	@Override
	public void writeConfig(String key, Object value)
	{
		switch (key)
		{
			case "accuracy": ((Configurable)ppu).writeConfig("accuracy", value); break;
			case "fps": ((Configurable)video).writeConfig("fps", value); break;
		}
	}
    
    private void insertCartridge()
    {
    	region = Configuration.config.region;
        //expansion = Configuration.config.expansion_port;
        if (region == Region.Autodetect)
        {
            region = (((Configurable)cartridge).readConfig("region").equals("ntsc") ? Region.NTSC : Region.PAL);
        }
        
        ((Configurable)smp).writeConfig("region", region == Region.NTSC ? "ntsc" : "pal");
        ((Configurable)ppu).writeConfig("region", region == Region.NTSC ? "ntsc" : "pal");
        ((Configurable)counter_cpu).writeConfig("region", region == Region.NTSC ? "ntsc" : "pal");
        
        //Audio.audio.coprocessor_enable(false);
        
        // coprocessors enable

        // add cpu coprocessors

        ((Clockable)input).clock(1L);
        
        reset();
    }
}
