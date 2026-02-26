/*
package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;

public class AddressIndex extends IROperand{

    public final IROperand base;          // Location | StackFrameOperand | VirtualRegister (pointer)
    public final IROperand index;         // nullable; if null => pure const offset
    public final int       scale;         // element size in bytes (>=1)
    public final int       constOffset;   // field offset or extra byte-displacement
    public final TypeSpecifier elemType;  // type of the thing we’ll load/store

    public AddressIndex(IROperand base, IROperand index, int scale, int constOffset, TypeSpecifier elemType) {
        super(Type.ADDRESS_INDEX);
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.constOffset = constOffset;
        this.elemType = elemType;
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return elemType;
    }

    @Override
    public String toString() {
        return "(" + base.toString() + (index != null ? " + " + index.toString() + " * " + scale : "") + (constOffset != 0 ? (" + ") + constOffset : "") + ")";
    }
}
*/
