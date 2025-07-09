package com.lnc.cc.common;

import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

public class BaseSymbol{
    private final Token token;
    private final TypeSpecifier type;
    private boolean isForward;
    private Scope scope;
    private String flatSymbolName;

    private boolean isParameter;
    private final int parameterIndex;
    private final boolean isStatic;


    private BaseSymbol(Token token, TypeSpecifier type, boolean isForward, boolean isParameter, int parameterIndex, boolean isStatic) {
        this.token = token;
        this.type = type;
        this.isForward = isForward;
        this.isParameter = isParameter;
        this.parameterIndex = parameterIndex;
        this.isStatic = isStatic;
    }

    public static BaseSymbol forward(Token token, TypeSpecifier type, boolean isStatic) {
        return new BaseSymbol(token, type, true, false, -1, isStatic);
    }

    public static BaseSymbol parameter(Token token, TypeSpecifier type, int parameterIndex) {
        return new BaseSymbol(token, type, false, true, parameterIndex, false);
    }

    public static BaseSymbol static_(Token token, TypeSpecifier type, boolean isForward) {
        return new BaseSymbol(token, type, isForward, false, -1, true);
    }

    public static BaseSymbol variable(Token token, TypeSpecifier type, boolean isForward, boolean isStatic) {
        return new BaseSymbol(token, type, isForward, false, -1, isStatic);
    }


    public String getName() {
        return flatSymbolName == null ? token.lexeme : flatSymbolName;
    }

    public TypeSpecifier getType() {
        return type;
    }

    public boolean isForward() {
        return isForward;
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
        return isForward == symbol.isForward && token.equals(symbol.token) && type.equals(symbol.type);
    }

    @Override
    public int hashCode() {
        int result = token.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + Boolean.hashCode(isForward);
        return result;
    }

    public void setForward(boolean forward) {
        this.isForward = forward;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return String.format("%s %s [%s]", token.lexeme, flatSymbolName == null ? "" : "( " + flatSymbolName + ")", type);
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
        return isStatic;
    }
}
