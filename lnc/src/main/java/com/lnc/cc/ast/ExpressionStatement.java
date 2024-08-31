package com.lnc.cc.ast;



public class ExpressionStatement extends Statement {
    private final Expression expression;

    public ExpressionStatement(Expression expression) {
        super(Type.EXPRESSION_STMT);
        this.expression = expression;
    }

}
