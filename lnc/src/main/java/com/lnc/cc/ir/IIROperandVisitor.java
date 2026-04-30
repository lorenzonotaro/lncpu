package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.*;

public interface IIROperandVisitor<T> {
    T visit(ImmediateOperand immediateOperand);

    T visit(VirtualRegister vr);

    T visit(Location location);

    T visit(AddressOf addressOf);

    T visit(SizedCast sizedCast);

    default T visit(VaPop vaPop){
        return null; // TODO: implement va_pop
    }

    T visit(ComposeOperand composeOperand);
}
