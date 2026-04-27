package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;

import java.util.List;

public class VaPop extends IROperand{
    // tempVr is the temporary register used to perform BP adjustment
    private final VirtualRegister tempVr;
    private final TypeSpecifier typeSpecifier;

    public VaPop(VirtualRegister tempVr, TypeSpecifier typeSpecifier) {
        super(Type.VA_POP);
        this.tempVr = tempVr;
        this.typeSpecifier = typeSpecifier;
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
    public String toString() {
        return "va_pop(" + typeSpecifier.allocSize() + ")";
    }

    @Override
    public List<VirtualRegister> getVRReads() {
        return List.of(tempVr);
    }

    @Override
    public List<VirtualRegister> getVRWrites() {
        return List.of(tempVr);
    }
}
