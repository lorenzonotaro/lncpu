package com.lnc.cc.ir;

public class Label extends IRInstruction {
    private final IRBlock block;

    public Label(IRBlock block) {
        super();
        this.block = block;
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }

    @Override
    public String toString() {
        return block + ": ";
    }
}
