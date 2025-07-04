package com.lnc.cc.ast;

public class BlockStatement extends ScopedStatement {

    public final Statement[] statements;

    public BlockStatement(Statement[] statements) {
        super(Statement.Type.BLOCK);
        this.statements = statements;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.visit(this);
    }

}
