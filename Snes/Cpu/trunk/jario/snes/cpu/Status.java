package jario.snes.cpu;

public class Status
{
	public boolean interrupt_pending;
    public int interrupt_vector;

    public int clock_count;
    public int line_clocks;

    //timing
    public boolean irq_lock;

    public int dram_refresh_position;
    public boolean dram_refreshed;

    public int hdma_init_position;
    public boolean hdma_init_triggered;

    public int hdma_position;
    public boolean hdma_triggered;

    public boolean nmi_valid;
    public boolean nmi_line;
    public boolean nmi_transition;
    public boolean nmi_pending;
    public boolean nmi_hold;

    public boolean irq_valid;
    public boolean irq_line;
    public boolean irq_transition;
    public boolean irq_pending;
    public boolean irq_hold;

    public boolean reset_pending;

    //DMA
    public boolean dma_active;
    public int dma_counter;
    public int dma_clocks;
    public boolean dma_pending;
    public boolean hdma_pending;
    public boolean hdma_mode;  //0 = init, 1 = run

    //MMIO
    //$2140-217f
    public byte[] port = new byte[4];

    //$2181-$2183
    public int wram_addr;

    //$4016-$4017
    public int joypad1_bits;
    public int joypad2_bits;

    //$4200
    public boolean nmi_enabled;
    public boolean hirq_enabled, virq_enabled;
    public boolean auto_joypad_poll;

    //$4201
    public int pio;

    //$4202-$4203
    public int wrmpya;
    public int wrmpyb;

    //$4204-$4206
    public int wrdiva;
    public int wrdivb;

    //$4207-$420a
    public int hirq_pos;
    public int virq_pos;

    //$420d
    public int rom_speed;

    //$4214-$4217
    public int rddiv;
    public int rdmpy;

    //$4218-$421f
    public int joy1l, joy1h;
    public int joy2l, joy2h;
    public int joy3l, joy3h;
    public int joy4l, joy4h;
}
