package com.lnc.cc.types;

public abstract class AbstractSubscriptableType extends NumericalTypeSpecifier {
    protected final TypeSpecifier baseType;

    public AbstractSubscriptableType(Type type, TypeSpecifier baseType, boolean signed) {
        super(type, true, signed);
        this.baseType = baseType;
    }

    public TypeSpecifier getBaseType() {
        return baseType;
    }
}
