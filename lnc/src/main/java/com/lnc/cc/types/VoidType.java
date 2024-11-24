package com.lnc.cc.types;

public class VoidType extends TypeSpecifier {

    public VoidType(){
        super(Type.VOID);
    }

    @Override
    public String toString(){
        return "void";
    }

    @Override
    public int typeSize() {
        return 0;
    }
}
