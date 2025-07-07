package com.lnc.cc.optimization;

import com.lnc.cc.ir.operands.StructMemberAccess;
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
    public PropValue visit(StructMemberAccess structMemberAccess) {
        return PropValue.unknown();
    }

    @Override
    public PropValue visit(ArrayElementAccess arrayElementAccess) {
        return PropValue.unknown();
    }

    @Override
    public PropValue visit(StackFrameOperand stackFrameOperand) {
        return PropValue.unknown();
    }
}
