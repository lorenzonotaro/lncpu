package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.Location;

import java.util.Collection;
import java.util.List;

public class Store extends IRInstruction {
    private Location dest;
    private IROperand value;

    public Store(IROperand value, Location dest) {
        super();
        this.dest = dest;
        this.value = value;
    }

    public Location getDest() {
        return dest;
    }

    public IROperand getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("store %s <- %s", dest.toString(), value.toString());
    }

    @Override
    public Collection<IROperand> getReads() {
        return List.of(value);
    }

    @Override
    public Collection<IROperand> getWrites() {
        return List.of(dest);
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (value.equals(oldOp)) {
            value = newOp;
        } else if (dest.equals(oldOp)) {
            if (newOp instanceof Location newOpLocation) {
                dest = newOpLocation;
            } else {
                throw new RuntimeException("Cannot replace destination of Store with non-Location operand: " + newOp);
            }
        }
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
