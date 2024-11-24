package com.lnc.cc.ir;

public abstract class IROperand {
    public Type type;

    public IROperand(Type type){
        this.type = type;
    }

    public abstract String asm();

    public enum Type{
        IMMEDIATE, VIRTUAL_REGISTER, DERIVED_LOCATION, LOCATION
    }
}
