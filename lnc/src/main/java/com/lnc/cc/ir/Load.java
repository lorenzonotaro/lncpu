package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.Location;
import com.lnc.cc.ir.operands.VirtualRegister;

public class Load extends IRInstruction {
    private final VirtualRegister vr;
    private final Location operand;

    public Load(VirtualRegister vr, Location operand) {
        super();
        this.vr = vr;
        this.operand = operand;

        vr.checkReleased();

        this.operand.addRead(this);
        this.vr.addWrite(this);

    }

    public VirtualRegister getVR() {
        return vr;
    }

    public Location getOperand() {
        return operand;
    }

    @Override
    public String toString() {
        return "load " + operand + " -> " + vr;
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
