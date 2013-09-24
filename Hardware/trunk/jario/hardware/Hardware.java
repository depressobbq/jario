package jario.hardware;

public interface Hardware
{
	public void connect(int port, Hardware hw);
	public void reset();
}
