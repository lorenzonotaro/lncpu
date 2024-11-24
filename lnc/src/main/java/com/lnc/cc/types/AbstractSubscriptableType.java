package com.lnc.cc.types;

public abstract class AbstractSubscriptableType extends TypeSpecifier {
    protected final TypeSpecifier baseType;

    public AbstractSubscriptableType(Type type, TypeSpecifier baseType) {
        super(type);
        this.baseType = baseType;
    }

    public TypeSpecifier getBaseType() {
        return baseType;
    }
}
