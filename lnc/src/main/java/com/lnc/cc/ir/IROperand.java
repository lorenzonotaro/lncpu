package com.lnc.cc.ir;

public class IROperand {
    public Type type;

    public IROperand(Type type){
        this.type = type;
    }

    public enum Type{
        IMMEDIATE, VIRTUAL_REGISTER, LOCATION
    }
}
