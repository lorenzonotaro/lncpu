package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;

public class StackFrameOperand extends IROperand {
    private final int offset;
    private TypeSpecifier typeSpecifier;
    private final String paramName;

    public StackFrameOperand(TypeSpecifier type, String paramName, int offset) {
        super(Type.STACK_FRAME_OPERAND);
        this.typeSpecifier = type;
        this.paramName = paramName;
        this.offset = offset;
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
        return "[BP " + offset + "] (" + paramName + ")";
    }

    public int getOffset() {
        return offset;
    }
}
