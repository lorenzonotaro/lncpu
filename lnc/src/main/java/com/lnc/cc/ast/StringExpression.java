package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public class StringExpression extends Expression {

    public StringExpression(Token token) {
        super(Type.STRING, token);
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
