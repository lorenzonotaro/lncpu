package com.lnc.cc.ir.operands;

import com.lnc.cc.common.BaseSymbol;
import com.lnc.cc.types.StorageLocation;
import com.lnc.cc.types.TypeSpecifier;

public class StaticSymbolLocation extends StaticLocation {
    private final BaseSymbol symbol;

    public StaticSymbolLocation(BaseSymbol symbol) {
        super(LocationType.SYMBOL);
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
    public TypeSpecifier getTypeSpecifier() {
        return symbol.getTypeSpecifier();
    }

    @Override
    public StorageLocation getPointerKind() {
        return symbol.getTypeSpecifier().storageLocation();
    }

    @Override
    public String compose() {
        return symbol.getAsmName();
    }
}
