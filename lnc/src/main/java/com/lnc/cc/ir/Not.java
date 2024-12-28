package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public class Not extends IRInstruction {
    private final IROperand operand;

    public Not(IROperand operand) {
        this.operand = operand;

        if(operand.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)operand).checkReleased();
        }

        if(operand instanceof IReferenceable rop){
            rop.addWrite(this);
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
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
