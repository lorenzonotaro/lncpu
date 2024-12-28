package com.lnc.cc.ir.operands;

import com.lnc.cc.common.AbstractSymbol;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.ir.ReferenceableIROperand;

public class Location extends ReferenceableIROperand {
    private AbstractSymbol symbol;

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
    public String asm() {
        return "[" + symbol.getAsmName() + "]";
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.accept(this);
    }
}
