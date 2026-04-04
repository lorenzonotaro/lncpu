package com.lnc.cc.ir.operands;

import com.lnc.cc.types.StorageLocation;
import com.lnc.cc.types.TypeSpecifier;

public class StaticDerivedLocation extends StaticLocation{
    private final StaticLocation base;
    private final int offset;
    private final TypeSpecifier typeSpecifier;

    public StaticDerivedLocation(StaticLocation base, int offset, TypeSpecifier typeSpecifier) {
        super(StaticDerivedLocation.LocationType.STATIC_DERIVED);
        this.base = base;
        this.offset = offset;
        this.typeSpecifier = typeSpecifier;
    }

    @Override
    public String compose() {
        return "(" + base.compose() + offset + ")";
    }

    @Override
    public StorageLocation getPointerKind() {
        return base.getPointerKind();
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return typeSpecifier;
    }

    @Override
    public String toString() {
        return compose();
    }

    public StaticLocation getBase() {
        return base;
    }

    public int getOffset() {
        return offset;
    }
}
