package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public class NumericalExpression extends Expression {

    public final Token token;

    public final int value;

    public NumericalExpression(Token token) {
        super(Type.NUMERICAL);
        this.token = token;
        this.value = (Integer) token.literal;
    }

    public NumericalExpression(Token token, int value) {
        super(Type.NUMERICAL);
        this.token = token;
        this.value = value;
    }

}
