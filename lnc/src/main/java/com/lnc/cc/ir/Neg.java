package com.lnc.cc.ir;

public class Neg extends IRInstruction {
    private final IROperand operand;

    public Neg(IROperand operand) {
        super();
        this.operand = operand;

        if(operand.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)operand).checkReleased();
        }

    }

    public IROperand getOperand() {
        return operand;
    }

    @Override
    public String toString() {
        return String.format("neg %s", operand);
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
