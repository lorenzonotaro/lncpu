package com.lnc.cc.ir;

public class Label extends IRInstruction {
    public final IRBlock block;

    public Label(IRBlock block) {
        super();
        this.block = block;
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return block + ": ";
    }
}
