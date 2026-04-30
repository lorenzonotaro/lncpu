package com.lnc.cc.optimization.ir;

import com.lnc.cc.ir.operands.AddressOf;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.ir.operands.*;

public class ConstantPropagationEvaluator implements IIROperandVisitor<PropValue> {
    @Override
    public PropValue visit(ImmediateOperand immediateOperand) {
        return PropValue.constant(immediateOperand.getValue());
    }

    @Override
    public PropValue visit(VirtualRegister vr) {
        return PropValue.unknown();
    }

    @Override
    public PropValue visit(Location location) {
        return PropValue.unknown();
    }

    @Override
    public PropValue visit(AddressOf addressOf) {
        return PropValue.unknown();
    }

    @Override
    public PropValue visit(SizedCast sizedCast) {
        PropValue operand = sizedCast.getOperand().accept(this);
        if (!operand.isConstant()) {
            return PropValue.unknown();
        }

        int sourceSize = sizedCast.getOperand().getTypeSpecifier().allocSize();
        int targetSize = sizedCast.getTypeSpecifier().allocSize();
        int value = operand.valueOr(0);

        if (sourceSize == targetSize) {
            return PropValue.constant(value);
        }

        if (sourceSize == 1 && targetSize == 2) {
            return PropValue.constant(value & 0xFF);
        }

        if (sourceSize == 2 && targetSize == 1) {
            int truncated = sizedCast.getByteSelection() == SizedCast.ByteSelection.HIGH ? (value >>> 8) & 0xFF : value & 0xFF;
            return PropValue.constant(truncated);
        }

        return PropValue.unknown();
    }

    @Override
    public PropValue visit(ComposeOperand composeOperand) {
        return PropValue.unknown();
    }
}
