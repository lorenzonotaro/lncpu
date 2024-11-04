package com.lnc.cc.types;

public class I8Type extends TypeSpecifier {

    public I8Type(){
        super(Type.I8);
    }

    @Override
    public String toString(){
        return "int";
    }

    @Override
    public int size() {
        return 1;
    }
}
