package com.lnc.cc.types;

public abstract class NumericalTypeSpecifier extends TypeSpecifier {
    private final boolean signed;

    public NumericalTypeSpecifier(Type type, boolean primitive, boolean signed) {
        super(type, primitive);
        this.signed = signed;
    }

    public boolean isSigned() {
        return signed;
    }
}
