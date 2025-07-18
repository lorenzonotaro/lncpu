package com.lnc.cc.ir.operands;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.PointerType;
import com.lnc.cc.types.TypeSpecifier;

import java.util.List;

public class Deref extends IROperand {

    private IROperand target;

    public Deref(IROperand target) {
        super(Type.DEREF);
        this.target = target;
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return ((PointerType) target.getTypeSpecifier()).getBaseType();
    }

    @Override
    public String toString() {
        return "*" + target.toString();
    }

    public IROperand getTarget() {
        return target;
    }

    @Override
    public List<VirtualRegister> getVRReads() {
        return target.getVRReads();
    }

    public void setTarget(IROperand target) {
        this.target = target;
    }
}
