package com.lnc.cc.ir.operands;

import com.lnc.cc.common.AbstractSymbol;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.PointerType;
import com.lnc.cc.types.TypeSpecifier;

public class AddressOf extends IROperand {
    private final AbstractSymbol symbol;

    public AddressOf(AbstractSymbol symbol) {
        super(Type.ADDRESS_OF);
        this.symbol = symbol;
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.accept(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return new PointerType(symbol.getType());
    }

    @Override
    public String toString() {
        return "&" + symbol.getAsmName();
    }
}
