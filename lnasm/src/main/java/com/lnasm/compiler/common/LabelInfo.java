package com.lnasm.compiler.common;

public record LabelInfo(Token token, String name) {
    public LabelInfo(Token token){
        this(token, token.lexeme);
    }
}
