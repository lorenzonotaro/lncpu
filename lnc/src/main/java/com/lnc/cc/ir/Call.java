package com.lnc.cc.ir;

public class Call extends IRInstruction {
    private final IROperand returnTarget;

    private final IROperand callee;
    private final IROperand[] arguments;

    public Call(IROperand returnTarget, IROperand callee, IROperand[] arguments) {
        super();
        this.returnTarget = returnTarget;
        this.callee = callee;
        this.arguments = arguments;

        if(returnTarget != null && returnTarget.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)returnTarget).checkReleased();
        }

        if(callee.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)callee).checkReleased();
        }

        for (int i = 0; i < arguments.length; i++) {
            if(arguments[i].type == IROperand.Type.VIRTUAL_REGISTER){
                ((VirtualRegister)arguments[i]).checkReleased();
            }
        }
    }

    public IROperand getReturnTarget() {
        return returnTarget;
    }

    public IROperand[] getArguments() {
        return arguments;
    }

    public IROperand getCallee() {
        return callee;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("call ");

        if(returnTarget != null)
            sb.append(returnTarget).append(" <- ");

        sb.append(callee);

        for (int i = 0; i < arguments.length; i++) {
            sb.append(", ");
            sb.append(arguments[i]);

        }
        return sb.toString();
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
