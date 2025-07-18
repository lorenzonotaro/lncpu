package com.lnc.cc.types;

public class PointerType extends AbstractSubscriptableType {

    private final PointerKind pointerKind;

    public PointerKind getPointerKind() {
        return pointerKind;
    }

    // near/far type
    public enum PointerKind {
        NEAR, FAR
    }

    public PointerType(TypeSpecifier baseType, PointerKind kind) {
        super(Type.POINTER, baseType);
        this.pointerKind = kind;
    }

    @Override
    public String toString(){
        String baseType = this.baseType.toString();
        return baseType + (baseType.endsWith("*") ? "" : " ") + "*";
    }

    @Override
    public int typeSize() {
        return pointerKind == PointerKind.FAR ? 2 : 1;
    }

    @Override
    public boolean compatible(TypeSpecifier other) {
        if (other instanceof PointerType otherPointer) {
            return baseType.compatible(otherPointer.baseType);
        }

        if(other instanceof ArrayType otherArray) {
            return baseType.compatible(otherArray.getBaseType());
        }

        if(other instanceof UI8Type otherUI8) {
            return true;
        }

        if(other instanceof UI16Type otherUI16) {
            return pointerKind == PointerKind.FAR;
        }

        return false;
    }
}
