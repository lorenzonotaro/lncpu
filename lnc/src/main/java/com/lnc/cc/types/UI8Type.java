package com.lnc.cc.types;

public class UI8Type extends TypeSpecifier {

    public UI8Type(){
        super(Type.UI8);
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
        if (other instanceof UI8Type) {
            return true;
        }


        return false;
    }
}
