package com.lnc.cc.ir;

public class Ret extends IRInstruction {
    private final IROperand value;

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
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
