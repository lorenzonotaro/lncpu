package com.lnc.cc.types;

public class Declarator {
    public final TypeSpecifier typeSpecifier;
    public final TypeQualifier typeQualifier;

    public Declarator(TypeQualifier typeQualifier, TypeSpecifier typeSpecifier){
        this.typeSpecifier = typeSpecifier;
        this.typeQualifier = typeQualifier;
    }

    public static Declarator wrapPointer(Declarator declarator){
        return new Declarator(declarator.typeQualifier, new PointerType(declarator.typeSpecifier));
    }

}
