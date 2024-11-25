package com.lnc.cc.ir.operands;

import com.lnc.cc.common.AbstractSymbol;
import com.lnc.cc.common.BaseSymbol;
import com.lnc.cc.ir.ReferencableIROperand;

public class Location extends ReferencableIROperand {
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
}
