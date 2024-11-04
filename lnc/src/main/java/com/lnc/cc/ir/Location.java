package com.lnc.cc.ir;

import com.lnc.cc.common.Symbol;

public class Location extends ReferencableIROperand {
    private Symbol symbol;

    public Location(Symbol symbol) {
        super(Type.LOCATION);
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol.getName();
    }

    @Override
    public String asm() {
        return "[" + symbol.getFlatSymbolName() + "]";
    }
}
