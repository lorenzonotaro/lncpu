package com.lnc.cc.codegen;

public enum RegisterClass {

    ANY(new Register[]{Register.RD, Register.RC, Register.RB, Register.RA}),
    SHIFT(new Register[]{Register.RA}),
    RETURN(new Register[]{Register.RD});

    private final Register[] registers;

    RegisterClass(Register[] registers) {
        this.registers = registers;
    }

    public Register[] getRegisters() {
        return registers;
    }
}
