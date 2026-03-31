package com.lnc.cc.ir.operands;

import com.lnc.cc.types.StorageLocation;
import com.lnc.cc.types.TypeSpecifier;

public class StackFrameLocation extends Location {

    public enum OperandType {
        LOCAL, PARAMETER
    }

    private final OperandType operandType;
    private int offset;
    private final TypeSpecifier typeSpecifier;

    public OperandType getOperandType() {
        return operandType;
    }

    public StackFrameLocation(TypeSpecifier typeSpecifier, OperandType operandType, int offset) {
        super(LocationType.STACK_FRAME);
        this.typeSpecifier = typeSpecifier;
        this.operandType = operandType;
        this.offset = offset;
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return typeSpecifier;
    }

    @Override
    public StorageLocation getPointerKind() {
        return StorageLocation.FAR; // stack frame operands are always far pointers when the address is requested
    }

    @Override
    public String toString() {
        return "[BP + " + offset + "]";
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
