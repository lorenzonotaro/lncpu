package com.lnc.cc.ast;

public class ForStatement extends ScopedStatement {
    public final Statement initializer;
    public final Expression condition;
    public final Expression increment;
    public final Statement body;

    public ForStatement(Statement initializer, Expression condition, Expression increment, Statement body) {
        super(Statement.Type.FOR);
        this.initializer = initializer;
        this.condition = condition;
        this.increment = increment;
        this.body = body;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }
}
