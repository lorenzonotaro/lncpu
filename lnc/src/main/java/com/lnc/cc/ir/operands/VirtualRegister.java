package com.lnc.cc.ir.operands;

import com.lnc.cc.codegen.Register;
import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;

public class VirtualRegister extends IROperand {

    private final TypeSpecifier typeSpecifier;

    private RegisterClass registerClass;

    private final int registerNumber;

    private Register assignedPhysicalRegister;

    public VirtualRegister(int registerNumber, TypeSpecifier typeSpecifier) {
        super(Type.VIRTUAL_REGISTER);
        this.typeSpecifier = typeSpecifier;
        this.registerNumber = registerNumber;
        registerClass = RegisterClass.ANY;
    }

    public int getRegisterNumber() {
        return registerNumber;
    }

    @Override
    public String toString() {
        return "r" + registerNumber + (registerClass == null ? "" : " {" + registerClass + "}");
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
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return typeSpecifier;
    }

    @Override
    public boolean equals(Object other){
        return other instanceof VirtualRegister vr && (this == vr );
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(registerNumber);
    }
}
