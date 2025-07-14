package com.lnc.cc.ir.operands;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.PointerType;
import com.lnc.cc.types.TypeSpecifier;

import java.util.List;

public class Deref extends IROperand {

    private final VirtualRegister target;

    public Deref(VirtualRegister target) {
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

    @Override
    public List<VirtualRegister> getVRReferences() {
        return List.of(target);
    }

    public VirtualRegister getTarget() {
        return target;
    }
}
