package com.lnc.cc.ast;

import com.lnc.cc.common.ASTVisitor;
import com.lnc.common.frontend.Token;

public class ContinueStatement extends Statement {

    public final Token token;

    public ContinueStatement(Token token) {
        super(Type.CONTINUE);
        this.token = token;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }
}
