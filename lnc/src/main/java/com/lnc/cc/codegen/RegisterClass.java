package com.lnc.cc.codegen;

import java.util.Set;

public enum RegisterClass {

    ANY(new Register[]{Register.RC, Register.RB, Register.RA, Register.RD}),

    BYTEPARAM_1(new Register[]{Register.RA}),

    BYTEPARAM_2(new Register[]{Register.RB}),

    BYTEPARAM_3(new Register[]{Register.RC}),

    BYTEPARAM_4(new Register[]{Register.RD}),

    WORDPARAM_1(new Register[]{Register.RC, Register.RD}),

    RET_BYTE(new Register[]{Register.RB}),

    RET_WORD(new Register[]{Register.RC, Register.RD}),

    SHIFT(new Register[]{Register.RA}),
    INDEX(new Register[]{Register.RD}),
    RETURN(new Register[]{Register.RB});

    private final Register[] registers;

    RegisterClass(Register[] registers) {
        this.registers = registers;
    }

    public Register[] getRegisters() {
        return registers;
    }

    public int getSize() {
        return registers.length;
    }

    public Register next(Set<Register> neighborAssignments) {
        for (Register register : registers) {
            if(!neighborAssignments.contains(register)) {
                return register;
            }
        }
        return null;
    }
}
