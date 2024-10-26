package com.lnc.cc.ast;

public class SubscriptExpression extends Expression {
    public final Expression left;
    public final Expression index;

    public SubscriptExpression(Expression left, Expression index) {
        super(Type.SUBSCRIPT, left.token);
        this.left = left;
        this.index = index;
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
