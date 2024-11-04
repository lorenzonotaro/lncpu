package com.lnc.assembler.common;

import com.lnc.common.frontend.Token;

public record LabelInfo(Token token, String name) {
    public LabelInfo(Token token){
        this(token, token.lexeme);
    }
}
