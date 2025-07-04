package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.I8Type;
import com.lnc.cc.types.TypeSpecifier;

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
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.accept(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return new I8Type();
    }
}
