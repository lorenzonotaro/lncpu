package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public class BreakStatement extends Statement {

    public final Token token;

    public BreakStatement(Token token) {
        super(Type.BREAK);
        this.token = token;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }
}
