package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.I8Type;
import com.lnc.cc.types.TypeSpecifier;

public class ImmediateOperand extends IROperand {
    private final int value;
    private final TypeSpecifier typeSpecifier;

    public ImmediateOperand(int value, TypeSpecifier typeSpecifier) {
        super(Type.IMMEDIATE);
        this.value = value;
        this.typeSpecifier = typeSpecifier;
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
        return visitor.visit(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return typeSpecifier;
    }
}
