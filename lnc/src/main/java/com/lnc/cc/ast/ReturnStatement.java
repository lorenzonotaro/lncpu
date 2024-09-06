package com.lnc.cc.ast;



public class ReturnStatement extends Statement {

    public final Expression expression;

    public ReturnStatement(Expression expression) {
        super(Statement.Type.RETURN);
        this.expression = expression;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }
}
