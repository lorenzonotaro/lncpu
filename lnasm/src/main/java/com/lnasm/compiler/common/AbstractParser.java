package com.lnasm.compiler.common;

import java.util.Arrays;

public abstract class AbstractParser<T> {

    protected static CompileException error(Token token, String errorMsg) {
        return new CompileException(errorMsg, token);
    }

    protected boolean check(Token.Type... types) {
        if (isAtEnd()) return false;
        Token.Type peekType = peek().type;
        return Arrays.stream(types).anyMatch(t -> t == peekType);
    }

    protected Token consume(String errorMsg, Token.Type... types) {
        if (check(types)) return advance();
        else throw error(peek(), errorMsg);
    }

    protected boolean match(Token.Type type) {
        if (!isAtEnd() && peek().type == type) {
            advance();
            return true;
        }
        return false;
    }

    protected abstract Token advance();

    protected abstract Token previous();

    protected abstract Token peek();

    public abstract T getResult();

    public abstract boolean parse();

    protected abstract boolean isAtEnd();
}
