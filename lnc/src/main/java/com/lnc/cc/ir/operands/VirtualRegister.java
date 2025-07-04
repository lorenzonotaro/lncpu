package com.lnc.cc.ir.operands;

import com.lnc.cc.codegen.Register;
import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.ir.ReferenceableIROperand;

public class VirtualRegister extends ReferenceableIROperand {

    private static long virtualRegisterCounter = 0;

    private RegisterClass registerClass;

    private final int registerNumber;

    private final long instanceId;

    private boolean released;

    private Register assignedPhysicalRegister;

    public VirtualRegister(int registerNumber) {
        super(Type.VIRTUAL_REGISTER);
        this.instanceId = virtualRegisterCounter++;
        this.registerNumber = registerNumber;
        this.released = false;
        registerClass = RegisterClass.ANY;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VirtualRegister that = (VirtualRegister) o;
        return registerNumber == that.registerNumber && instanceId == that.instanceId;
    }

    public void setRegisterClass(RegisterClass registerClass) {
        this.registerClass = registerClass;
    }

    public RegisterClass getRegisterClass() {
        return registerClass;
    }

    public Register getAssignedPhysicalRegister() {
        return assignedPhysicalRegister;
    }

    public void setAssignedPhysicalRegister(Register assignedPhysicalRegister) {
        this.assignedPhysicalRegister = assignedPhysicalRegister;
    }

    @Override
    public String asm() {
        return assignedPhysicalRegister == null ? "UNASSIGNED" : assignedPhysicalRegister.toString();
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.accept(this);
    }
}
