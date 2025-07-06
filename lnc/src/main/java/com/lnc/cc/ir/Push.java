package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.List;

public class Push extends IRInstruction {
    private IROperand arg;

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

    public IROperand getArg() {
        return arg;
    }

    public void setArg(IROperand arg) {
        this.arg = arg;
    }
}
