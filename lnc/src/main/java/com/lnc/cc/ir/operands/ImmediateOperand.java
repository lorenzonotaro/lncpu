package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;

public class ImmediateOperand extends IROperand {
    private final byte value;

    public ImmediateOperand(byte value) {
        super(Type.IMMEDIATE);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public String asm() {
        return String.format("0x%02X", value & 0xFF);
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.accept(this);
    }
}
