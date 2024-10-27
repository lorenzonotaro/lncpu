package com.lnc.cc.ir;

public class VirtualRegister extends IROperand {
    private final int registerNumber;

    public VirtualRegister(int registerNumber) {
        super(Type.VIRTUAL_REGISTER);
        this.registerNumber = registerNumber;
    }

    public int getRegisterNumber() {
        return registerNumber;
    }

    @Override
    public String toString() {
        return "r" + registerNumber;
    }
}
