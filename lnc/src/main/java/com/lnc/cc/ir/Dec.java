package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public class Dec extends IRInstruction {
    private final IROperand operand;

    public Dec(IROperand operand) {
        super();
        this.operand = operand;

        if(operand.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)operand).checkReleased();
        }

        if(operand instanceof IReferenceable rop){
            rop.addWrite(this);
        }
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.accept(this);
    }

    @Override
    public String toString() {
        return "dec " + operand;
    }

    public IROperand getOperand() {
        return operand;
    }
}
