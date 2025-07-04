package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public class NumericalExpression extends Expression {

    public final int value;

    public NumericalExpression(Token token) {
        super(Type.NUMERICAL, token);
        this.value = (Integer) token.literal;
    }

    public NumericalExpression(Token token, int value) {
        super(Type.NUMERICAL, token);
        this.value = value;
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
