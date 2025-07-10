package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.Collection;
import java.util.List;

public class LoadParam extends IRInstruction{

    private IROperand dest;

    public LoadParam(IROperand dest) {
        super();
        this.dest = dest;
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.accept(this);
    }

    @Override
    public String toString() {
        return "loadparam " + getDest();
    }

    @Override
    public Collection<IROperand> getReads() {
        return List.of();
    }

    @Override
    public Collection<IROperand> getWrites() {
        return List.of(dest);
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (dest.equals(oldOp)) {
            dest = newOp;
        }
    }

    public IROperand getDest() {
        return dest;
    }
}
