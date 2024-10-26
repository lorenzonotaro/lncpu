package com.lnc.cc.ast;

import com.lnc.cc.anaylsis.Scope;

public class BlockStatement extends ScopedStatement {

    public final Statement[] statements;

    public BlockStatement(Statement[] statements) {
        super(Statement.Type.BLOCK);
        this.statements = statements;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }

}
