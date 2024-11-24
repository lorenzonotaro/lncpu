package com.lnc.cc.types;

public class PointerType extends AbstractSubscriptableType {

    public PointerType(TypeSpecifier baseType){
        super(Type.POINTER, baseType);
    }

    @Override
    public String toString(){
        String baseType = this.baseType.toString();
        return baseType + (baseType.endsWith("*") ? "" : " ") + "*";
    }

    @Override
    public int typeSize() {
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
