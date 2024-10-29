package com.lnc.cc.ir;

public class Not extends IRInstruction {
    private final IROperand operand;

    public Not(IROperand operand) {
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
        return "not " + operand;
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
