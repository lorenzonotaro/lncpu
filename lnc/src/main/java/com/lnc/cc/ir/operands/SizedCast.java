package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;

import java.util.List;

public class SizedCast extends IROperand {
    private IROperand operand;
    private final TypeSpecifier targetType;
    private final ByteSelection byteSelection;

    public enum ByteSelection {
        LOW,
        HIGH
    }


    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return targetType;
    }

    @Override
    public String toString() {
        if (operand.getTypeSpecifier().allocSize() == 2 && targetType.allocSize() == 1 && byteSelection == ByteSelection.HIGH) {
            return "(" + targetType.toString() + ",high) " + operand.toString();
        }
        return "(" + targetType.toString() + ") " + operand.toString();
    }


    public SizedCast(IROperand operand, TypeSpecifier targetType) {
        this(operand, targetType, ByteSelection.LOW);
    }

    public SizedCast(IROperand operand, TypeSpecifier targetType, ByteSelection byteSelection) {
        super(Type.SIZED_CAST);
        this.operand = operand;
        this.targetType = targetType;
        this.byteSelection = byteSelection == null ? ByteSelection.LOW : byteSelection;
    }
    public IROperand getOperand() {
        return operand;
    }

    public void setOperand(IROperand operand) {
        this.operand = operand;
    }

    public ByteSelection getByteSelection() {
        return byteSelection;
    }

    @Override
    public List<VirtualRegister> getVRReads() {
        return operand instanceof VirtualRegister vr ? List.of(vr) : operand.getVRReads();
    }
}
