package com.lnc.cc.types;

public class PointerType extends TypeSpecifier {

    private final TypeSpecifier baseType;

    public PointerType(TypeSpecifier baseType){
        super(Type.POINTER);
        this.baseType = baseType;
    }

    public TypeSpecifier getBaseType(){
        return baseType;
    }

    @Override
    public String toString(){
        return baseType.toString() + " *";
    }

    @Override
    public int size() {
        return 8;
    }
}
