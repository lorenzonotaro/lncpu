package com.lnc.cc.types;

public class PointerType extends AbstractSubscriptableType {

    private final StorageLocation pointerKind;

    public static TypeSpecifier wrap(TypeSpecifier ts, boolean hasConst, StorageLocation sl) {
        return new PointerType(ts, hasConst, sl);
    }

    public StorageLocation getPointerKind() {
        return pointerKind;
    }

    public PointerType(TypeSpecifier baseType, boolean isPointerConst, StorageLocation kind) {
        super(Type.POINTER, baseType);
        this.pointerKind = kind;
        this.isConst = isPointerConst;
    }

    @Override
    public String toString(){
        String baseType = this.baseType.toString();
        return baseType + (baseType.endsWith("*") ? "" : " ") + "*";
    }

    @Override
    public int typeSize() {
        return pointerKind == StorageLocation.FAR ? 2 : 1;
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
            return pointerKind == StorageLocation.FAR;
        }

        return false;
    }
}
