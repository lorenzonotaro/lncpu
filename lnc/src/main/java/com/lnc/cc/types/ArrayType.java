package com.lnc.cc.types;

public class ArrayType extends AbstractSubscriptableType {

    public final TypeSpecifier baseType;
    public final int size;

    public ArrayType(TypeSpecifier baseType, int size) {
        super(Type.ARRAY, baseType);
        this.baseType = baseType;
        this.size = size;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArrayType array) {
            return baseType.equals(array.baseType) && size == array.size;
        }
        return false;
    }

    @Override
    public boolean compatible(TypeSpecifier other) {
        if (other instanceof ArrayType array) {
            return baseType.compatible(array.baseType);
        }else if(other instanceof PointerType pointer){
            return baseType.compatible(pointer.getBaseType());
        }
        return false;
    }

    @Override
    public String toString() {
        return baseType + "[" + size + "]";
    }

    @Override
    public int typeSize(){
        return baseType.typeSize();
    }

    @Override
    public int allocSize() {
        return baseType.typeSize() * size;
    }
}
