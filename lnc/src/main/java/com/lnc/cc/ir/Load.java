package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.Location;

public class Load extends IRInstruction {
    private final IROperand dest;
    private final Location src;

    public Load(IROperand dest, Location src) {
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
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
