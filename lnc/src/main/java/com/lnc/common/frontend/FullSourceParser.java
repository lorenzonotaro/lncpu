package com.lnc.common.frontend;

public abstract class FullSourceParser<T> extends AbstractParser<T> {
    protected final Token[] tokens;
    protected int index;

    public FullSourceParser(Token[] tokens) {
        this.tokens = tokens;
        this.index = 0;
    }

    @Override
    protected boolean isAtEnd() {
        return index >= tokens.length;
    }

    @Override
    public Token advance() {
        if (isAtEnd())
            throw error(previous(), "unexpected end of file");
        return tokens[index++];
    }

    @Override
    protected Token previous() {
        return tokens[index - 1];
    }

    @Override
    public Token peek() {
        if (isAtEnd())
            throw error(previous(), "unexpected end of file");
        return tokens[index];
    }
}
