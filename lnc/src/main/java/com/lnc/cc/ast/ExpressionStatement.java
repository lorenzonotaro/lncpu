package com.lnc.cc.ast;



public class ExpressionStatement extends Statement {
    public final Expression expression;

    public ExpressionStatement(Expression expression) {
        super(Type.EXPRESSION_STMT);
        this.expression = expression;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
