package com.lnc.cc.ast;

public class CallExpression extends Expression {
    public final Expression callee;
    public final Expression[] arguments;

    public CallExpression(Expression callee, Expression[] arguments) {
        super(Type.CALL, callee.token);
        this.callee = callee;
        this.arguments = arguments;
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
