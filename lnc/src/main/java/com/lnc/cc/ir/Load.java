package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.VirtualRegister;

public class Load extends IRInstruction {
    private final ReferenceableIROperand dest;
    private final ReferenceableIROperand src;

    public Load(ReferenceableIROperand dest, ReferenceableIROperand src) {
        super();
        this.dest = dest;
        this.src = src;

        if(dest instanceof VirtualRegister vr)
            vr.checkReleased();

        if(src instanceof VirtualRegister vr)
            vr.checkReleased();

        this.src.addRead(this);
        this.dest.addWrite(this);

    }

    public ReferenceableIROperand getDest() {
        return dest;
    }

    public ReferenceableIROperand getSrc() {
        return src;
    }

    @Override
    public String toString() {
        return "load " + src + " -> " + dest;
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
