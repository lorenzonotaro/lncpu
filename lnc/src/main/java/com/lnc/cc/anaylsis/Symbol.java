package com.lnc.cc.anaylsis;

import com.lnc.cc.types.Declarator;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

import java.util.Objects;

public class Symbol {
    private final Token token;
    private final TypeSpecifier type;
    private final boolean isForward;

    public Symbol(Token token, TypeSpecifier type, boolean isForward) {
        this.token = token;
        this.type = type;
        this.isForward = isForward;
    }

    public String getName() {
        return token.lexeme;
    }

    public TypeSpecifier getType() {
        return type;
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
        return isForward == symbol.isForward && token.equals(symbol.token) && type.equals(symbol.type);
    }

    @Override
    public int hashCode() {
        int result = token.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + Boolean.hashCode(isForward);
        return result;
    }
}
