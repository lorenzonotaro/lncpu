package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.PointerType;
import com.lnc.cc.types.TypeSpecifier;

import java.util.List;

public class AddressOf extends IROperand {
    private final Location operand;
    public AddressOf(Location operand) {
        super(Type.ADDRESS_OF);
        this.operand = operand;
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        // &x -> pointer to x. We set isPointerConst to true even though it doesn't matter, as the & operator
        // is always a rvalue (cannot be assigned to)
        return new PointerType(operand.getTypeSpecifier(), true, operand.getPointerKind());
    }

    @Override
    public String toString() {
        return "&" + operand;
    }


    public IROperand getOperand() {
        return operand;
    }
}
