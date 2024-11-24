package com.lnc.cc.ir.operands;

public class DynamicArrayIndexingLocation extends Location {
    private final VirtualRegister index;

    public DynamicArrayIndexingLocation(Location location, VirtualRegister index) {
        super(location.getSymbol());
        this.index = index;
    }

    public IROperand getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "[" + index.asm() + "]";
    }

    @Override
    public String asm() {
        return "[" + index.asm() + "]";
    }
}
