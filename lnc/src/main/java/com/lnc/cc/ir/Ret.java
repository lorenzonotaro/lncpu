package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;

public class Ret extends IRInstruction {
    private final IROperand value;

    public Ret(IROperand value) {
        this.value = value;

        if(value != null && value.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)value).checkReleased();
            ((VirtualRegister)value).setRegisterClass(RegisterClass.RETURN);
        }

        if(value instanceof ReferencableIROperand rop){
            rop.addRead(this);
        }

    }

    public IROperand getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ret" + (value != null ? " " + value : "");
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
