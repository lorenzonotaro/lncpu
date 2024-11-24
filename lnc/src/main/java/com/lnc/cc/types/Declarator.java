package com.lnc.cc.types;

import com.lnc.common.frontend.Token;

public record Declarator(TypeQualifier typeQualifier, TypeSpecifier typeSpecifier) {

    public static Declarator wrapPointer(Declarator declarator) {
        return new Declarator(declarator.typeQualifier, new PointerType(declarator.typeSpecifier));
    }

    public static Declarator wrapArray(Declarator declarator, int size) {
        return new Declarator(declarator.typeQualifier, new ArrayType(declarator.typeSpecifier, size));
    }
}
