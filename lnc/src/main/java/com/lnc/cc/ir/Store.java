package com.lnc.cc.ir;

public class Store extends IRInstruction {
    private final Location dest;
    private final IROperand value;

    public Store(Location dest, IROperand value) {
        super();
        this.dest = dest;
        this.value = value;

        if(value.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)value).checkReleased();
        }
    }

    public Location getDest() {
        return dest;
    }

    public IROperand getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("store %s -> %s", value, dest);
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
