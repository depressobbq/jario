package jario.hardware;

public interface Bus32bit
{
	public int read32bit(int address);
	public void write32bit(int address, int data);
}
