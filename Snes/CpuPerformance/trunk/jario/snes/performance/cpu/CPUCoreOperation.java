package jario.snes.performance.cpu;

public class CPUCoreOperation
{
	public interface CPUCoreOp { public void Invoke(CPUCoreOpArgument args); }
	
	private CPUCoreOp op;
    private CPUCoreOpArgument args;

    public CPUCoreOperation(CPUCoreOp op, CPUCoreOpArgument args)
    {
        this.op = op;
        this.args = args;
    }

    public void Invoke()
    {
        op.Invoke(args);
    }
}
