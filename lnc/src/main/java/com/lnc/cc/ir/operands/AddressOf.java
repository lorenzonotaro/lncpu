package com.lnc.cc.ir.operands;

import com.lnc.cc.common.AbstractSymbol;
import com.lnc.cc.ir.IIROperandVisitor;

public class AddressOf extends IROperand {
    private final AbstractSymbol symbol;

    public AddressOf(AbstractSymbol symbol) {
        super(Type.IMMEDIATE);
        this.symbol = symbol;
    }

    @Override
    public String asm() {
        return symbol.getAsmName();
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.accept(this);
    }
}
