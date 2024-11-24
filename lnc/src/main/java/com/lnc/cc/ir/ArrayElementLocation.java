package com.lnc.cc.ir;

import com.lnc.cc.types.AbstractSubscriptableType;

public class ArrayElementLocation extends Location {
    private final int index;
    private final AbstractSubscriptableType arrayTypeDecl;

    public ArrayElementLocation(Location base, int index) {
        super(base.getSymbol());

        if(!(base.getSymbol().getType() instanceof AbstractSubscriptableType)) {
            throw new IllegalArgumentException("Base must be an array");
        }

        this.arrayTypeDecl = (AbstractSubscriptableType) base.getSymbol().getType();

        this.index = index;
    }

    @Override
    public String toString() {
        return String.format("%s[%d]", getSymbol().getName(), index);
    }

    @Override
    public String asm() {
        return String.format("[%s + (%d * %d)]", getSymbol().getFlatSymbolName(), arrayTypeDecl.getBaseType().typeSize(), index);
    }
}
