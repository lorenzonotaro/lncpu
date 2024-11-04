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
    public int size() {
        return 1;
    }
}
