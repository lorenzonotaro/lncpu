package com.lnc.cc.ir;

public class VirtualRegister extends IROperand {

    private static long virtualRegisterCounter = 0;

    private final int registerNumber;

    private final long instanceId;

    private boolean released;

    public VirtualRegister(int registerNumber) {
        super(Type.VIRTUAL_REGISTER);
        this.instanceId = virtualRegisterCounter++;
        this.registerNumber = registerNumber;
        this.released = false;
    }

    public int getRegisterNumber() {
        return registerNumber;
    }

    @Override
    public String toString() {
        return "r" + registerNumber;
    }

    @Override
    public int hashCode() {
        return registerNumber;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public void release() {
        released = true;
    }

    public void checkReleased() {
        if(released) {
            throw new IllegalStateException("Register already released");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VirtualRegister that = (VirtualRegister) o;
        return registerNumber == that.registerNumber && instanceId == that.instanceId;
    }
}
