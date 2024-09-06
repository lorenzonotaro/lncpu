package com.lnc.cc.ast;

import com.lnc.cc.types.Declarator;
import com.lnc.common.frontend.Token;


public class FunctionDeclaration extends Declaration {

    public final VariableDeclaration[] parameters;
    public final BlockStatement body;

    public FunctionDeclaration(Declarator declarator, Token name, VariableDeclaration[] parameters, BlockStatement body) {
        super(Declaration.Type.FUNCTION, declarator, name);
        this.parameters = parameters;
        this.body = body;
    }

    public boolean isForwardDeclaration(){
        return body == null;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }
}
