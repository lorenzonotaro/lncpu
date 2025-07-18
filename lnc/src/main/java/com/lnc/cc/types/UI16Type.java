package com.lnc.cc.types;

public class UI16Type extends TypeSpecifier {
    public UI16Type() {
        super(Type.UI16, true);
    }

    @Override
    public int typeSize() {
        return 2;
    }
}
