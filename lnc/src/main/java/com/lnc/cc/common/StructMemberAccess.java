package com.lnc.cc.common;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.types.StructFieldEntry;
import com.lnc.cc.types.TypeSpecifier;

public class StructMemberAccess extends IROperand{
    private final IROperand base;
    private final StructFieldEntry field;

    public StructMemberAccess(IROperand base, StructFieldEntry field) {
        super(Type.STRUCT_MEMBER_ACCESS);
        this.base = base;
        this.field = field;
    }

    public IROperand getBase()        { return base; }
    public StructFieldEntry getField()     { return field; }
    public int getByteOffset()        { return field.getOffset(); }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.accept(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return field.getField().declarator.typeSpecifier();
    }

    @Override
    public String toString() {
        return base.toString() + "." + field.getField().name.lexeme;
    }
}
