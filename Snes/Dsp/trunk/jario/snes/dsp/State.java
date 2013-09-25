package jario.snes.dsp;

public class State
{
	int[] regs = new int[128];

    ModuloArray[] echo_hist = new ModuloArray[2];  //echo history keeps most recent 8 samples
    int echo_hist_pos;

    boolean every_other_sample;  //toggles every sample
    int kon;                  //KON value when last checked
    int noise;
    int counter;
    int echo_offset;          //offset from ESA in echo buffer
    int echo_length;          //number of bytes that echo_offset will stop at

    //hidden registers also written to when main register is written to
    int new_kon;
    int endx_buf;
    int envx_buf;
    int outx_buf;

    //temporary state between clocks

    //read once per sample
    int t_pmon;
    int t_non;
    int t_eon;
    int t_dir;
    int t_koff;

    //read a few clocks ahead before used
    int t_brr_next_addr;
    int t_adsr0;
    int t_brr_header;
    int t_brr_byte;
    int t_srcn;
    int t_esa;
    int t_echo_disabled;

    //internal state that is recalculated every sample
    int t_dir_addr;
    int t_pitch;
    int t_output;
    int t_looped;
    int t_echo_ptr;

    //left/right sums
    int[] t_main_out = new int[2];
    int[] t_echo_out = new int[2];
    int[] t_echo_in = new int[2];

    public State(int echo_hist_size)
    {
        for (int i = 0; i < echo_hist.length; i++)
        {
            echo_hist[i] = new ModuloArray(echo_hist_size);
        }
    }
}
