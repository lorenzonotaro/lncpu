package com.lnc.cc.codegen;

import java.util.LinkedHashSet;
import java.util.Set;

public enum RegisterClass {

    ANY(Set.of(Register.RC, Register.RB, Register.RA, Register.RD)),

    BYTEPARAM_1(Set.of(Register.RA)),

    BYTEPARAM_2(Set.of(Register.RB)),

    BYTEPARAM_3(Set.of(Register.RC)),

    BYTEPARAM_4(Set.of(Register.RD)),

    WORDPARAM_1(Set.of(Register.RCRD)),

    RET_BYTE(Set.of(Register.RB)),

    RET_WORD(Set.of(Register.RCRD)),

    SHIFT(Set.of(Register.RA)),
    INDEX(Set.of(Register.RD)),
    RETURN(Set.of(Register.RB));

    private final Set<Register> registers;

    RegisterClass(Set<Register> registers) {
        this.registers = new LinkedHashSet<>(registers);
    }

    public Set<Register> getRegisters() {
        return registers;
    }

    public int getSize() {
        return registers.size();
    }

    public Register next(Set<Register> neighborAssignments) {
        for (Register register : registers) {
            if(!neighborAssignments.contains(register)) {
                return register;
            }
        }
        return null;
    }

    public boolean isSingleton() {
        return registers.size() == 1;
    }

    public Register onlyRegister() {
        return registers.size() == 1 ? registers.iterator().next() : null;
    }
}
