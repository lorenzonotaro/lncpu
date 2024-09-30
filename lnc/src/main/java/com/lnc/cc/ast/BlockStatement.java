package com.lnc.cc.ast;

import com.lnc.cc.anaylsis.Scope;

public class BlockStatement extends Statement {

    private Scope scope;

    public final Statement[] statements;

    public BlockStatement(Statement[] statements) {
        super(Statement.Type.BLOCK);
        this.statements = statements;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        if(this.scope != null){
            throw new IllegalStateException("Scope already set");
        }
        this.scope = scope;
    }
}
