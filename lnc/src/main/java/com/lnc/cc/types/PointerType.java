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
