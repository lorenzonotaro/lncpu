package com.lnc.cc.types;

public class UI8Type extends NumericalTypeSpecifier {

    public UI8Type(){
        super(Type.UI8, true, false);
    }

    @Override
    public String toString(){
        return "unsigned int";
    }

    @Override
    public int typeSize() {
        return 1;
    }

    @Override
    public boolean compatible(TypeSpecifier other) {
        return other instanceof UI8Type;
    }

    @Override
    public TypeSpecifier copy() {
        return new UI8Type();
    }
}
