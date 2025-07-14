package com.lnc.cc.codegen;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class RegisterClass {

    public static RegisterClass ANY = new RegisterClass(Set.of(Register.RC, Register.RB, Register.RA, Register.RD));

    public static RegisterClass BYTEPARAM_1 = new RegisterClass(Set.of(Register.RA));

    public static RegisterClass BYTEPARAM_2 = new RegisterClass(Set.of(Register.RB));

    public static RegisterClass BYTEPARAM_3 = new RegisterClass(Set.of(Register.RC));

    public static RegisterClass BYTEPARAM_4 = new RegisterClass(Set.of(Register.RD));

    public static RegisterClass WORDPARAM_1 = new RegisterClass(Set.of(Register.RCRD));

    public static RegisterClass RET_BYTE = new RegisterClass(Set.of(Register.RB));

    public static RegisterClass RET_WORD = new RegisterClass(Set.of(Register.RCRD));

    public static RegisterClass SHIFT = new RegisterClass(Set.of(Register.RA));

    public static RegisterClass DEREF = new RegisterClass(Set.of(Register.RD));

    public static RegisterClass RETURN = new RegisterClass(Set.of(Register.RB));

    private final Set<Register> registers;

    private RegisterClass(Collection<Register> registers) {
        this.registers = new LinkedHashSet<>(registers);
    }

    public Set<Register> getRegisters() {
        return registers;
    }

    public int getSize() {
        return registers.size();
    }

    public boolean isSingleton() {
        return registers.size() == 1;
    }

    public Register onlyRegister() {
        return registers.size() == 1 ? registers.iterator().next() : null;
    }

    public static RegisterClass of(Collection<Register> registers) {
        return new RegisterClass(registers);
    }

    @Override
    public String toString() {
        return "[" + String.join(", ", registers.stream().map(Register::toString).toList()) + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterClass that = (RegisterClass) o;
        return registers.equals(that.registers);
    }

    @Override
    public int hashCode() {
        return registers.hashCode();
    }
}
