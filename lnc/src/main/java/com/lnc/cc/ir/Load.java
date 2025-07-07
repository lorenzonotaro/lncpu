package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.Location;

import java.util.Collection;
import java.util.List;

public class Load extends IRInstruction {
    private IROperand dest;
    private Location src;

    public Load(Location src, IROperand dest) {
        super();
        this.dest = dest;
        this.src = src;
    }

    public IROperand getDest() {
        return dest;
    }

    public Location getSrc() {
        return src;
    }

    @Override
    public String toString() {
        return String.format("load %s <- %s", dest, src);
    }

    @Override
    public Collection<IROperand> getReads() {
        return List.of(src);
    }

    @Override
    public Collection<IROperand> getWrites() {
        return List.of(dest);
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (dest.equals(oldOp)) {
            dest = newOp;
        } else if (src.equals(oldOp)) {
            if(newOp instanceof Location newOpLocation) {
                src = newOpLocation;
            }else{
                throw new RuntimeException("Cannot replace source of Load with non-Location operand: " + newOp);
            }
        }
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
