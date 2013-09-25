package jario.snes.smp;

public class SMPCoreOpResult
{
	private int result;

	public SMPCoreOpResult(int result)
	{
		this.result = result;
	}

	public int result_byte()
	{
		return result & 0xFF;
	}

	public int result_ushort()
	{
		return result & 0xFFFF;
	}
}
