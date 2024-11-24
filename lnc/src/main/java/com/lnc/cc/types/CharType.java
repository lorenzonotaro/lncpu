package com.lnc.cc.types;

public class CharType extends TypeSpecifier {

    public CharType(){
        super(Type.CHAR);
    }

    @Override
    public String toString(){
        return "char";
    }

    @Override
    public int typeSize() {
        return 1;
    }
}
