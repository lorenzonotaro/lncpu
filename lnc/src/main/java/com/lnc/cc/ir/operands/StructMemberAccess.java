package com.lnc.cc.ir.operands;

import com.lnc.cc.types.*;

public class StructMemberAccess extends Location{
    private final Location base;
    private final StructFieldEntry field;

    public StructMemberAccess(Location base, StructFieldEntry field) {
        super(LocationType.STRUCT_MEMBER);
        this.base = base;
        this.field = field;
    }

    public IROperand getBase()        { return base; }
    public StructFieldEntry getField()     { return field; }
    public int getByteOffset()        { return field.getOffset(); }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        // the member storage location is the same as the base storage location
        // so we need to adjust the type specifier from the struct declaration to reflect this (making a copy of the type specifier)
        return field.getField().declarator.typeSpecifier().withStorageLocation(base.getPointerKind());
    }

    @Override
    public String toString() {
        return base.toString() + "." + field.getField().name.lexeme;
    }

    @Override
    public StorageLocation getPointerKind() {
        return base.getPointerKind();
        // struct member pointers are always the same as the base pointer, if the struct is addressed
        // by a far pointer, the member pointer is far too
    }
}
