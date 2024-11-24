package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public class Neg extends IRInstruction {
    private final IROperand operand;

    public Neg(IROperand operand) {
        super();
        this.operand = operand;

        if(operand.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)operand).checkReleased();
        }

        if(operand instanceof ReferencableIROperand rop){
            rop.addWrite(this);
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
