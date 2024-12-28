package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.*;

public interface IIROperandVisitor<T> {
    T accept(ImmediateOperand immediateOperand);

    T accept(VirtualRegister vr);

    T accept(RegisterDereference rd);

    T accept(Location location);

    T accept(AddressOf addressOf);
}
