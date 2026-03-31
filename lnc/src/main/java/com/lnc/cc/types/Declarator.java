package com.lnc.cc.types;

public record Declarator(StorageQualifier storageQualifier, TypeSpecifier typeSpecifier) {
    public static Declarator wrapArray(Declarator declarator, int size) {
        return new Declarator(declarator.storageQualifier, new ArrayType(declarator.typeSpecifier, size));
    }
}
