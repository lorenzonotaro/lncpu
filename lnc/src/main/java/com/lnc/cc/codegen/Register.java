package com.lnc.cc.codegen;

import java.util.Arrays;

public enum Register {
    RA(1), RB(1), RC(1), RD(1), RCRD(2, "RC:RD", RC, RD);

    private final int size;
    private final Register[] compound;
    private String regName;

    Register(int size) {
        this.size = size;
        this.compound = null;
        this.regName = null;
    }

    Register(int size, Register... compound){
        this(size, null, compound);
    }

    Register(int size, String regName, Register... compound) {
        this.regName = regName;
        this.size = size;
        this.compound = compound;
    }

    public int getSize() {
        return size;
    }

    public boolean isCompound() {
        return compound != null;
    }

    public Register[] getComponents() {
        if (compound == null) {
            return new Register[]{this};
        }
        return compound;
    }

    public String getRegName() {
        return regName != null ? regName : this.name();
    }

    public static Register[] getCompoundRegisters() {
        return Arrays.stream(Register.values()).filter(Register::isCompound).toArray(Register[]::new);
    }
}
