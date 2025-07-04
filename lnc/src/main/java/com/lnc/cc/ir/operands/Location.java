package com.lnc.cc.ir.operands;

import com.lnc.cc.common.AbstractSymbol;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;

public class Location extends IROperand {
    private final AbstractSymbol symbol;

    public Location(AbstractSymbol symbol) {
        super(Type.LOCATION);
        this.symbol = symbol;
    }

    public AbstractSymbol getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol.getAsmName();
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return symbol.getType();
    }
}
