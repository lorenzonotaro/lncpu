package com.lnc.cc.types;

public class I16Type extends NumericalTypeSpecifier {
    public I16Type() {
        super(Type.I16, true, true);
    }

    @Override
    public int typeSize() {
        return 2;
    }

    @Override
    public TypeSpecifier copy() {
        return new I16Type();
    }

    @Override
    public String toString() {
        return "long int";
    }
}
