package jario.snes.cpu;

import jario.snes.cpu.CPUCoreOperation.CPUCoreOp;

public class CPUCoreOpArgument
{
	public CPUCoreOp op;
    public int x;
    public int y;

    public CPUCoreOpArgument(int x) { this.x = x; }
    public CPUCoreOpArgument(int x, int y) { this.x = x; this.y = y; }
    public CPUCoreOpArgument(CPUCoreOp op) { this.op = op; }
    public CPUCoreOpArgument(CPUCoreOp op, int x) { this.op = op; this.x = x; }
}
