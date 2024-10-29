package com.lnc.cc.ir;

public class Dec extends IRInstruction {
    private final IROperand operand;

    public Dec(IROperand operand) {
        super();
        this.operand = operand;

        if(operand.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)operand).checkReleased();
        }
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }

    @Override
    public String toString() {
        return "dec " + operand;
    }
}
