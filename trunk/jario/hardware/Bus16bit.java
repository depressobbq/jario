package jario.hardware;

public interface Bus16bit
{
	public short read16bit(int address);
	public void write16bit(int address, short data);
}
