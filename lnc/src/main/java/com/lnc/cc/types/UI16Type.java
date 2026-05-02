package com.lnc.cc.types;

public class UI16Type extends NumericalTypeSpecifier {
    public UI16Type() {
        super(Type.UI16, true, false);
    }

    @Override
    public int typeSize() {
        return 2;
    }

    @Override
    public TypeSpecifier copy() {
        return new UI16Type();
    }

    @Override
    public String toString() {
        return "unsigned long int";
    }


}
