package com.lnc.cc.ir.operands;

import com.lnc.cc.common.BaseSymbol;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.TypeSpecifier;

public class Location extends IROperand {
    private final BaseSymbol symbol;

    public Location(BaseSymbol symbol) {
        super(Type.LOCATION);
        this.symbol = symbol;
    }

    public BaseSymbol getSymbol() {
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
        return symbol.getTypeSpecifier();
    }
}
