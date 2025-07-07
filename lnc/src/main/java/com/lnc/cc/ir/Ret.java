package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.Collection;
import java.util.List;

public class Ret extends IRInstruction {
    private IROperand value;

    public Ret(IROperand value) {
        this.value = value;
    }

    public IROperand getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ret" + (value != null ? " " + value : "");
    }

    @Override
    public Collection<IROperand> getReads() {
        return value != null ? List.of(value) : List.of();
    }

    @Override
    public Collection<IROperand> getWrites() {
        return List.of();
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (value != null && value.equals(oldOp)) {
            value = newOp;
        } else if (value == null && oldOp == null) {
            value = newOp; // Allow replacing null with a new operand
        }
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    public void setValue(IROperand value) {
        if (value == null) {
            throw new IllegalArgumentException("Return value cannot be null");
        }
        this.value = value;
    }
}
