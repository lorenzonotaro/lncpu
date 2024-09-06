package com.lnc.cc.ast;

import com.lnc.cc.types.Declarator;
import com.lnc.common.frontend.Token;



public class VariableDeclaration extends Declaration {

    public final Expression initializer;

    public VariableDeclaration(Declarator declarator, Token name, Expression initializer) {
        super(Declaration.Type.VARIABLE, declarator, name);
        this.initializer = initializer;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }
}
