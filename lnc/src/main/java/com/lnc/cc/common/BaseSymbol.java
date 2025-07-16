package com.lnc.cc.common;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.types.TypeQualifier;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

public class BaseSymbol{
    private final Token token;
    private final TypeSpecifier typeSpecifier;
    private final TypeQualifier qualifier;
    private Scope scope;
    private String flatSymbolName;
    private final boolean isParameter;
    private final int parameterIndex;

    private BaseSymbol(Token token, TypeSpecifier typeSpecifier, TypeQualifier qualifier, boolean isParameter, int parameterIndex) {
        this.token = token;
        this.typeSpecifier = typeSpecifier;
        this.qualifier = qualifier;
        this.isParameter = isParameter;
        this.parameterIndex = parameterIndex;
    }

    public static BaseSymbol parameter(Token token, TypeSpecifier type, TypeQualifier qualifier, int parameterIndex) {
        return new BaseSymbol(token, type, qualifier, true, parameterIndex);
    }

    public static BaseSymbol variable(Token token, TypeSpecifier type, TypeQualifier qualifier) {
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
        return qualifier.equals(symbol.qualifier) && token.equals(symbol.token) && typeSpecifier.equals(symbol.typeSpecifier);
    }

    @Override
    public int hashCode() {
        int result = token.hashCode();
        result = 31 * result + typeSpecifier.hashCode();
        result = 31 * result + qualifier.hashCode();
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

    public boolean isStatic() {
        return qualifier.isStatic();
    }

    public boolean canResideInRegister() {
        // TODO: guard against address-of operator
        return !qualifier.isExtern() && !isParameter && !qualifier.isStatic() && typeSpecifier.allocSize() < 2 && typeSpecifier.allocSize() > 0;
    }

    public TypeQualifier getTypeQualifier() {
        return qualifier;
    }
}
