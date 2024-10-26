package com.lnc.cc.ast;

import com.lnc.cc.anaylsis.Scope;

public abstract class ScopedStatement extends Statement {
    private Scope scope;

    public ScopedStatement(Type type) {
        super(type);
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        if (this.scope != null) {
            throw new IllegalStateException("Scope already set");
        }
        this.scope = scope;
    }
}
