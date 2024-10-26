package com.lnc.cc.ir;

import com.lnc.cc.common.Scope;
import com.lnc.cc.ast.ScopedStatement;

public class IRBlock {
    private final IRBlock parent;
    private final ScopedStatement scopedStatement;
    private final Scope scope;

    public IRBlock(IRBlock parent, ScopedStatement scopedStatement) {
        this.parent = parent;
        this.scopedStatement = scopedStatement;
        this.scope = scopedStatement.getScope();
    }
}
