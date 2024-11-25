package com.lnc.cc.ir.operands;

import com.lnc.cc.common.AbstractSymbol;
import com.lnc.cc.types.TypeSpecifier;

public class IndirectAddressingSymbol extends AbstractSymbol {
    private final VirtualRegister index;
    private final TypeSpecifier type;

    public IndirectAddressingSymbol(TypeSpecifier type, VirtualRegister index) {

        this.type = type;

        index.checkReleased();

        this.index = index;
    }

    public IROperand getIndexRegister() {
        return index;
    }

    @Override
    public String toString() {
        return "[" + index.asm() + "]";
    }

    @Override
    public String getAsmName() {
        return "[" + index.asm() + "]";
    }

    @Override
    public TypeSpecifier getType() {
        return type;
    }
}
