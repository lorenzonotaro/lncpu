package com.lnc.cc.ir.operands;

public abstract class StaticLocation extends Location {
    public StaticLocation(LocationType type) {
        super(type);
    }

    public abstract String compose();
}
