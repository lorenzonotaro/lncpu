package com.lnc.cc.ast;

import com.lnc.cc.common.Scope;

public interface IScopedStatement {
    Scope getScope();

    void setScope(Scope scope);
}
