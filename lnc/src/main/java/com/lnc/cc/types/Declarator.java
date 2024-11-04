package com.lnc.cc.types;

public record Declarator(TypeQualifier typeQualifier, TypeSpecifier typeSpecifier) {

    public static Declarator wrapPointer(Declarator declarator) {
        return new Declarator(declarator.typeQualifier, new PointerType(declarator.typeSpecifier));
    }

}
