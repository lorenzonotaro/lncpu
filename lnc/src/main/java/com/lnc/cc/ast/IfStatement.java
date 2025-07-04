package com.lnc.cc.ast;



public class IfStatement extends Statement {

    public final Expression condition;
    public final Statement thenStatement;
    public final Statement elseStatement;

    public IfStatement(Expression condition, Statement thenStatement, Statement elseStatement) {
        super(Statement.Type.IF);
        this.condition = condition;
        this.thenStatement = thenStatement;
        this.elseStatement = elseStatement;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
