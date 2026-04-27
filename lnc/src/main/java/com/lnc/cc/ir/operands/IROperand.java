package com.lnc.cc.ir.operands;

import com.lnc.cc.codegen.LiveRange;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;

import java.util.List;

public abstract class IROperand {
    public Type type;

    public IROperand(Type type){
        this.type = type;
    }

    public abstract <T> T accept(IIROperandVisitor<T> visitor);

    public abstract TypeSpecifier getTypeSpecifier();

    public LiveRange getLiveRange() {
        // TODO: Implement live range for this operand
        return null;
    }

    public int spillCost() {
        // TODO: Implement spill cost for this operand
        return 0;
    }

    public abstract String toString();

    public List<VirtualRegister> getVRReads(){
        return List.of();
    }
    public List<VirtualRegister> getVRWrites(){
        return List.of();
    }

    public enum Type{
        IMMEDIATE,
        VIRTUAL_REGISTER,
        ADDRESS_OF,
        DEREF,
        LOCATION, SIZED_CAST,
    }
}
