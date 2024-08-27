package com.lnc.common.frontend;

import java.util.Arrays;

public abstract class AbstractParser<T> {

    protected static CompileException error(Token token, String errorMsg) {
        return new CompileException(errorMsg, token);
    }

    protected boolean check(TokenType... types) {
        if (isAtEnd()) return false;
        TokenType peekType = peek().type;
        return Arrays.stream(types).anyMatch(t -> t == peekType);
    }

    protected Token consume(String errorMsg, TokenType... types) {
        if (check(types)) return advance();
        else throw error(peek(), errorMsg);
    }

    protected boolean match(TokenType... types) {
        if(check(types)){
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
