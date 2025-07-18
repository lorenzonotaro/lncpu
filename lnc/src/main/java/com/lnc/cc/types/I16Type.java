package com.lnc.cc.types;

public class I16Type extends TypeSpecifier {
    public I16Type() {
        super(Type.I16, true);
    }

    @Override
    public int typeSize() {
        return 2;
    }
}
