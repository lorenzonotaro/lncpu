package com.lnc.cc.anaylsis;

import com.lnc.cc.types.Declarator;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

import java.util.Objects;

public class Symbol {
    private final Token token;
    private final TypeSpecifier type;
    private final boolean isFunction;
    private final boolean isForward;

    public Symbol(Token token, TypeSpecifier type, boolean isFunction, boolean isForward) {
        this.token = token;
        this.type = type;
        this.isFunction = isFunction;
        this.isForward = isForward;
    }

    public String getName() {
        return token.lexeme;
    }

    public TypeSpecifier getType() {
        return type;
    }

    public boolean isFunction() {
        return isFunction;
    }

    public boolean isForward() {
        return isForward;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Symbol symbol = (Symbol) o;
        return isFunction == symbol.isFunction && isForward == symbol.isForward && Objects.equals(token, symbol.token) && Objects.equals(type, symbol.type);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(token);
        result = 31 * result + Objects.hashCode(type);
        result = 31 * result + Boolean.hashCode(isFunction);
        result = 31 * result + Boolean.hashCode(isForward);
        return result;
    }
}
