package com.lnc.cc.common;

import com.lnc.cc.types.StorageQualifier;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

/**
 * Represents a base class for symbols used in the symbol table of a compiler or interpreter.
 * Symbols define variables, function parameters, or other named entities with type information, qualifiers,
 * and other metadata. This class provides common functionality for symbol manipulation and serves as a
 * foundation for more specific symbol types.
 */
public class BaseSymbol{
    private final Token token;
    private final TypeSpecifier typeSpecifier;
    private final StorageQualifier storageQualifier;
    private Scope scope;
    private String flatSymbolName;
    private final boolean isParameter;
    private final int parameterIndex;

    protected BaseSymbol(Token token, TypeSpecifier typeSpecifier, StorageQualifier storageQualifier, boolean isParameter, int parameterIndex) {
        this.token = token;
        this.typeSpecifier = typeSpecifier;
        this.storageQualifier = storageQualifier;
        this.isParameter = isParameter;
        this.parameterIndex = parameterIndex;
    }

    public static BaseSymbol parameter(Token token, TypeSpecifier type, StorageQualifier qualifier, int parameterIndex) {
        return new BaseSymbol(token, type, qualifier, true, parameterIndex);
    }

    public static BaseSymbol variable(Token token, TypeSpecifier type, StorageQualifier qualifier) {
        return new BaseSymbol(token, type, qualifier, false, -1);
    }

    public String getName() {
        return flatSymbolName == null ? token.lexeme : flatSymbolName;
    }

    public TypeSpecifier getTypeSpecifier() {
        return typeSpecifier;
    }

    public boolean isParameter() {
        return isParameter;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseSymbol symbol = (BaseSymbol) o;
        return storageQualifier.equals(symbol.storageQualifier) && token.equals(symbol.token) && typeSpecifier.equals(symbol.typeSpecifier);
    }

    @Override
    public int hashCode() {
        int result = token.hashCode();
        result = 31 * result + typeSpecifier.hashCode();
        result = 31 * result + storageQualifier.hashCode();
        return result;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return String.format("%s %s [%s]", token.lexeme, flatSymbolName == null ? "" : "( " + flatSymbolName + ")", typeSpecifier);
    }

    public void setFlatSymbolName(String name) {
        this.flatSymbolName = name;
    }

    public String getAsmName() {
        return this.flatSymbolName == null ? token.lexeme : flatSymbolName;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    public StorageQualifier getStorageQualifier() {
        return storageQualifier;
    }

    public boolean canResideInRegister() {
        // TODO: guard against address-of operator
        return
                !storageQualifier.isExtern() &&
                        !isParameter &&
                        !storageQualifier.isStatic() &&
                        typeSpecifier.allocSize() > 0 &&
                        typeSpecifier.allocSize() <= 2&&
                        !typeSpecifier.isAggregateType();
    }
}
