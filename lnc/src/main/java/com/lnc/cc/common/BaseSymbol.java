package com.lnc.cc.common;

import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

public class BaseSymbol extends AbstractSymbol {
    private final Token token;
    private final TypeSpecifier type;
    private boolean isForward;
    private Scope scope;
    private String flatSymbolName;

    private boolean isParameter;

    public BaseSymbol(Token token, TypeSpecifier type, boolean isForward, boolean isParameter) {
        this.token = token;
        this.type = type;
        this.isForward = isForward;
        this.isParameter = isParameter;
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

    @Override
    public String getAsmName() {
        return flatSymbolName;
    }
}
