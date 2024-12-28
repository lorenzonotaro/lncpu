package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public class Ret extends IRInstruction {
    private final IROperand value;

    public Ret(IROperand value) {
        this.value = value;

        if(value != null && value.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)value).checkReleased();
            ((VirtualRegister)value).setRegisterClass(RegisterClass.RETURN);
        }

        if(value instanceof IReferenceable rop){
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
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
