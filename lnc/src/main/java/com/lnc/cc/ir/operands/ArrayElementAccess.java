package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;

import java.util.Objects;

public final class ArrayElementAccess extends IROperand {
    private final IROperand      base;        // e.g. Location, another ArrayAccess, StructFieldAccess…
    private final IROperand      index;       // any IROperand (Immediate, Location, VR, etc.)
    private final TypeSpecifier elementType; // the type of each element
    private final int            stride;      // size in bytes of one element

    public ArrayElementAccess(IROperand base,
                       IROperand index,
                       TypeSpecifier elementType,
                       int stride) {
        super(Type.ARRAY_ACCESS);
        this.base        = Objects.requireNonNull(base);
        this.index       = Objects.requireNonNull(index);
        this.elementType = Objects.requireNonNull(elementType);
        this.stride      = stride;
    }

    public IROperand getBase()         { return base; }
    public IROperand getIndex()        { return index; }
    public TypeSpecifier getType()     { return elementType; }
    public int getStride()             { return stride; }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return elementType;
    }

    @Override
    public String toString() {
        return base.toString() + "[" + index.toString() + "]";
    }
}
