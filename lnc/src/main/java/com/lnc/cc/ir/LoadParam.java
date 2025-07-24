package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.Collection;
import java.util.List;

public class LoadParam extends IRInstruction{

    private IROperand originalReg;
    private final IROperand copyReg;

    public LoadParam(IROperand originalReg, IROperand copyReg) {
        super();
        this.originalReg = originalReg;
        this.copyReg = copyReg;
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.accept(this);
    }

    @Override
    public String toString() {
        return "loadparam " + copyReg + " <- " + getOriginalReg();
    }

    @Override
    public Collection<IROperand> getReadOperands() {
        return List.of(originalReg);
    }

    @Override
    public Collection<IROperand> getWriteOperands() {
        return List.of(copyReg);
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (originalReg.equals(oldOp)) {
            originalReg = newOp;
        }
    }

    public IROperand getOriginalReg() {
        return originalReg;
    }

    public IROperand getCopyReg() {
        return copyReg;
    }
}
