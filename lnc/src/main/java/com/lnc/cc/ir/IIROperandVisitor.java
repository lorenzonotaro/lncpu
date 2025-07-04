package com.lnc.cc.ir;

import com.lnc.cc.common.StructMemberAccess;
import com.lnc.cc.ir.operands.*;

public interface IIROperandVisitor<T> {
    T accept(ImmediateOperand immediateOperand);

    T accept(VirtualRegister vr);

    T accept(Location location);

    T accept(AddressOf addressOf);

    T accept(StructMemberAccess structMemberAccess);

    T accept(ArrayElementAccess arrayElementAccess);
}
