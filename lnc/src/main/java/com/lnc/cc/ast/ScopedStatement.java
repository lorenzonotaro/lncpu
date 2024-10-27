package com.lnc.cc.ast;

import com.lnc.cc.common.Scope;

public abstract class ScopedStatement extends Statement implements IScopedStatement {
    private Scope scope;

    public ScopedStatement(Type type) {
        super(type);
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public void setScope(Scope scope) {
        if (this.scope != null) {
            throw new IllegalStateException("Scope already set");
        }
        this.scope = scope;
    }
}
