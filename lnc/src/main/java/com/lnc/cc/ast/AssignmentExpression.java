package com.lnc.cc.ast;


import com.lnc.common.frontend.Token;

public class AssignmentExpression extends Expression {
    public final Expression left;
    public final Expression right;
    public Token operator;
    private final boolean isInitializer;

    public AssignmentExpression(Expression left, Token operator, Expression right, boolean isInitializer) {
        super(Expression.Type.ASSIGNMENT, operator);
        this.left = left;
        this.operator = operator;
        this.right = right;
        this.isInitializer = isInitializer;
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    public boolean isInitializer() {
        return isInitializer;
    }
}
