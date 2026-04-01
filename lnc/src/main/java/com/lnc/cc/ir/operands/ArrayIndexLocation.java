package com.lnc.cc.ir.operands;

import com.lnc.cc.types.StorageLocation;
import com.lnc.cc.types.TypeSpecifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ArrayIndexLocation extends Location {
    private final Location      base;        // e.g. Location, another ArrayAccess, StructFieldAccess…
    private final IROperand      index;       // any IROperand (Immediate, Location, VR, etc.)
    private final TypeSpecifier elementType; // the type of each element
    private final int            stride;      // size in bytes of one element

    public ArrayIndexLocation(Location base,
                              IROperand index,
                              TypeSpecifier elementType,
                              int stride) {
        super(LocationType.ARRAY_INDEX);
        this.base        = Objects.requireNonNull(base);
        this.index       = Objects.requireNonNull(index);
        this.elementType = Objects.requireNonNull(elementType);
        this.stride      = stride;
    }

    public Location getBase()         { return base; }
    public IROperand getIndex()        { return index; }
    public TypeSpecifier getType()     { return elementType; }
    public int getStride()             { return stride; }
    @Override
    public StorageLocation getPointerKind() {
        return base.getPointerKind();
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return elementType;
    }

    @Override
    public String toString() {
        return base.toString() + "[" + index.toString() + "]";
    }

    @Override
    public List<VirtualRegister> getVRReads() {
        List<VirtualRegister> reads = new ArrayList<>();
        reads.addAll(base.getVRReads());

        if (index instanceof VirtualRegister vr) {
            reads.add(vr);
        } else {
            reads.addAll(index.getVRReads());
        }

        return reads;
    }
}
