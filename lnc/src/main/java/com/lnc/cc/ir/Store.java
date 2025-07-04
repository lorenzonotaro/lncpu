package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.Location;
import com.lnc.cc.ir.operands.VirtualRegister;

public class Store extends IRInstruction {
    private final Location dest;
    private final IROperand value;

    public Store(Location dest, IROperand value) {
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
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
