package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;

public abstract class IROperand {
    public Type type;

    public IROperand(Type type){
        this.type = type;
    }

    public abstract String asm();

    public abstract <T> T accept(IIROperandVisitor<T> visitor);

    public enum Type{
        IMMEDIATE,
        VIRTUAL_REGISTER,
        DERIVED_LOCATION,
        REGISTER_DEREFERENCE, LOCATION
    }
}
