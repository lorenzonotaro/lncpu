package com.lnc.common.frontend;

import java.util.Arrays;

/**
 * AbstractParser provides a foundational base class for implementing parsers,
 * offering utility methods to analyze and consume a series of tokens. It includes
 * functions to check for token types, consume tokens based on expected types, and
 * handle errors during parsing. Subclasses are expected to provide specific behavior
 * for advancing through tokens, retrieving parsing results, and defining the logic
 * for parsing the input.
 *
 * @param <T> the type of the parsing result produced by the subclass.
 */
public abstract class AbstractParser<T> {

    protected static CompileException error(Token token, String errorMsg) {
        return new CompileException(errorMsg, token);
    }

    public boolean check(TokenType... types) {
        if (isAtEnd()) return false;
        TokenType peekType = peek().type;
        return Arrays.stream(types).anyMatch(t -> t == peekType);
    }

    public Token consume(String errorMsg, TokenType... types) {
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
