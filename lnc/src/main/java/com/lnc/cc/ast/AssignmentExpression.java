package com.lnc.cc.ast;


import com.lnc.common.frontend.Token;

public class AssignmentExpression extends Expression {
    public final Expression left;
    public final Expression right;
    public Token operator;

    public AssignmentExpression(Expression left, Token operator, Expression right) {
        super(Expression.Type.ASSIGNMENT, operator);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
