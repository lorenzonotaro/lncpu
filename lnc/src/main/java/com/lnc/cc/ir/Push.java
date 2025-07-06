package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.List;

public class Push extends IRInstruction {
    private final IROperand arg;

    public Push(IROperand arg) {
        this.arg = arg;
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "push " + arg.toString();
    }
}
