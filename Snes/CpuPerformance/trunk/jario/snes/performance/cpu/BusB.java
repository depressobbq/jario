package jario.snes.performance.cpu;

import jario.hardware.Bus8bit;
import jario.hardware.Hardware;

public class BusB extends Status implements Hardware, Bus8bit
{
	@Override
	public void connect(int port, Hardware hw) { }
	
	@Override
	public void reset() { }
	
	@Override
	public byte read8bit(int address)
	{
		return (byte)CPU.cpu.port_data[address & 0x3];
	}
	@Override
	public void write8bit(int address, byte data)
	{
		CPU.cpu.port_data[address & 0x3] = data & 0xFF;
	}
}
