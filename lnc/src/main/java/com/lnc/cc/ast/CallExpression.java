package com.lnc.cc.ast;

public class CallExpression extends Expression {
    public final Expression left;
    public final Expression[] arguments;

    public CallExpression(Expression left, Expression[] arguments) {
        super(Type.CALL);
        this.left = left;
        this.arguments = arguments;
    }

}
