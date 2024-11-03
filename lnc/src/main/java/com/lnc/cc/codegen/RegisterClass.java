package com.lnc.cc.codegen;

import com.lnc.cc.ir.VirtualRegister;

import java.util.Set;

public enum RegisterClass {

    ANY(new Register[]{Register.RD, Register.RC, Register.RB, Register.RA}),
    SHIFT(new Register[]{Register.RA}),
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
