package jario.snes.smp;

public class SMPCoreOperation
{
	public interface SMPCoreOp { public SMPCoreOpResult Invoke(SMPCoreOpArgument args); }
	
	private SMPCoreOp op;
    private SMPCoreOpArgument args;

    public SMPCoreOperation(SMPCoreOp op, SMPCoreOpArgument args)
    {
        this.op = op;
        this.args = args;
    }

    public final void Invoke()
    {
        op.Invoke(args);
    }
}
