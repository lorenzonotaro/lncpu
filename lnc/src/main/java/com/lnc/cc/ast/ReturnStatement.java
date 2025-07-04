package com.lnc.cc.ast;


import com.lnc.common.frontend.Token;

public class ReturnStatement extends Statement {

    public final Token token;
    public final Expression value;

    public ReturnStatement(Token token, Expression value) {
        super(Statement.Type.RETURN);
        this.token = token;
        this.value = value;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
