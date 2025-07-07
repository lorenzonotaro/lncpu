package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.Collection;
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

    @Override
    public Collection<IROperand> getReads() {
        return List.of(arg);
    }

    @Override
    public Collection<IROperand> getWrites() {
        return List.of();
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (arg.equals(oldOp)) {
            arg = newOp;
        }
    }

    public IROperand getArg() {
        return arg;
    }

    public void setArg(IROperand arg) {
        this.arg = arg;
    }
}
