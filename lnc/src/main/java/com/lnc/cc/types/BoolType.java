package com.lnc.cc.types;

public class BoolType extends TypeSpecifier {

    public BoolType(){
        super(Type.BOOL);
    }

    @Override
    public String toString(){
        return "bool";
    }
}
