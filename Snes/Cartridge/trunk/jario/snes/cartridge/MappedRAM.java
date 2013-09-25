package jario.snes.cartridge;

import jario.hardware.Bus8bit;
import jario.hardware.Hardware;

public class MappedRAM implements Hardware, Bus8bit
{
	private byte[] data_;
    private int size_;
    private boolean write_protect_;
    
    public MappedRAM()
    {
    	reset();
    }
	
    @Override
    public void connect(int port, Hardware hw) { }
    
    @Override
    public void reset()
    {
        if (data_ != null)
        {
            data_ = null;
        }

        size_ = -1;
        write_protect_ = false;
    }
    
    @Override
    public byte read8bit(int addr)
    {
        return data_[addr];
    }

    @Override
    public void write8bit(int addr, byte n)
    {
        if (!write_protect_)
        {
            data_[addr] = n;
        }
    }

    void map(byte[] source, int length)
    {
        reset();
        data_ = source;
        size_ = data_ != null && length > 0 ? length : -1;
    }

    void copy(byte[] data, int size)
    {
        if (data_ == null)
        {
            size_ = ((size & ~255) + (((size & 255)!=0 ? 1 : 0) << 8));
            data_ = new byte[size_];
        }
        System.arraycopy(data, 0, data_, 0, Math.min(size_, size));
    }

    void write_protect(boolean status)
    {
        write_protect_ = status;
    }

    byte[] data()
    {
        return data_;
    }

    int size()
    {
        return size_;
    }
}
