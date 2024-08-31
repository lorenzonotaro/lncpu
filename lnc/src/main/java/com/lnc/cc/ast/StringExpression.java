package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public class StringExpression extends Expression {
    public final Token token;

    public StringExpression(Token token) {
        super(Type.STRING);
        this.token = token;
    }

}
