package com.lnc.cc.ir;

import com.lnc.cc.common.StructMemberAccess;
import com.lnc.cc.ir.operands.*;

public interface IIROperandVisitor<T> {
    T visit(ImmediateOperand immediateOperand);

    T visit(VirtualRegister vr);

    T visit(Location location);

    T visit(AddressOf addressOf);

    T visit(StructMemberAccess structMemberAccess);

    T visit(ArrayElementAccess arrayElementAccess);

    T visit(StackFrameOperand stackFrameOperand);
}
