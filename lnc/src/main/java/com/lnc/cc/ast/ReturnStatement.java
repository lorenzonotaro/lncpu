package com.lnc.cc.ast;



public class ReturnStatement extends Statement {

    public final Expression value;

    public ReturnStatement(Expression value) {
        super(Statement.Type.RETURN);
        this.value = value;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }
}
