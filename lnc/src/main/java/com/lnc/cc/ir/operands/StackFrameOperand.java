package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

public class StackFrameOperand extends IROperand {

    public enum OperandType {
        LOCAL, PARAMETER
    }

    private final OperandType operandType;
    private int offset;
    private TypeSpecifier typeSpecifier;


    public OperandType getOperandType() {
        return operandType;
    }

    public StackFrameOperand(TypeSpecifier type, OperandType operandType, int offset) {
        super(Type.STACK_FRAME_OPERAND);
        this.typeSpecifier = type;
        this.operandType = operandType;
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
        return "[BP " + offset + "]";
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Stack frame operand offset cannot be negative");
        }
        this.offset = slot;
    }
}
