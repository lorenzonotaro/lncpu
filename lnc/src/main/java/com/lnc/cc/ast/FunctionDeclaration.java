package com.lnc.cc.ast;

import com.lnc.cc.common.Scope;
import com.lnc.cc.types.Declarator;
import com.lnc.common.frontend.Token;


public class FunctionDeclaration extends Declaration implements IScopedStatement {

    private Scope scope;

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

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public void setScope(Scope scope) {
        if(this.scope != null){
            throw new IllegalStateException("Scope already set");
        }
        this.scope = scope;
    }
}
