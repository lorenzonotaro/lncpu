package com.lnc.cc.ast;


public class AssignmentExpression extends Expression {
    public final Expression left;
    public final Expression right;

    public AssignmentExpression(Expression left, Expression right) {
        super(Expression.Type.ASSIGNMENT);
        this.left = left;
        this.right = right;
    }

}
