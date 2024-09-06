package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public class IdentifierExpression extends Expression {
    public final Token ident;

    public IdentifierExpression(Token ident) {
        super(Type.IDENTIFIER);
        this.ident = ident;
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
