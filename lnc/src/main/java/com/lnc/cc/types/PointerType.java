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
        String baseType = this.baseType.toString();
        return baseType + (baseType.endsWith("*") ? "" : " ") + "*";
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean compatible(TypeSpecifier other) {
        if (other instanceof PointerType otherPointer) {
            return baseType.compatible(otherPointer.baseType);
        }
        return false;
    }
}
