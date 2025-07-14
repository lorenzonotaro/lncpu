package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.Collection;
import java.util.List;

public class Move extends IRInstruction {
    private IROperand source;
    private IROperand dest;

    public Move(IROperand source, IROperand dest) {
        super();
        this.source = source;
        this.dest = dest;
    }


    public IROperand getSource() {
        return source;
    }

    public IROperand getDest() {
        return dest;
    }

    @Override
    public String toString() {
        return String.format("move %s <- %s", dest, source);
    }

    @Override
    public Collection<IROperand> getReadOperands() {
        return List.of(source);
    }

    @Override
    public Collection<IROperand> getWriteOperands() {
        return List.of(dest);
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (source.equals(oldOp)) {
            source = newOp;
        } else if (dest.equals(oldOp)) {
            dest = newOp;
        }
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    public void setSource(IROperand source) {
        this.source = source;
    }

    public void setDest(IROperand dest) {
        this.dest = dest;
    }
}
