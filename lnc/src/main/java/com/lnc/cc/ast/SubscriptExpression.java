package com.lnc.cc.ast;

public class SubscriptExpression extends Expression {
    public final Expression left;
    public final Expression index;

    public SubscriptExpression(Expression left, Expression index) {
        super(Type.SUBSCRIPT);
        this.left = left;
        this.index = index;
    }

}
