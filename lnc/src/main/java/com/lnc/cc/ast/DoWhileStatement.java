package com.lnc.cc.ast;

public class DoWhileStatement extends Statement{

    public final Expression condition;
    public final Statement body;

    public DoWhileStatement(Expression condition, Statement body) {
        super(Type.DO_WHILE);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
