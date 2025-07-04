package com.lnc.cc.ast;

import com.lnc.cc.common.Scope;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.types.Declarator;
import com.lnc.common.frontend.Token;


public class FunctionDeclaration extends Declaration implements IScopedStatement {

    public final Declarator declarator;
    public IRUnit unit;
    private Scope scope;

    public final VariableDeclaration[] parameters;
    public final BlockStatement body;

    public FunctionDeclaration(Declarator declarator, Token name, VariableDeclaration[] parameters, BlockStatement body) {
        super(Declaration.Type.FUNCTION, name);
        this.declarator = declarator;
        this.parameters = parameters;
        this.body = body;
    }

    public boolean isForwardDeclaration(){
        return body == null;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.visit(this);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(name.lexeme).append("(");
        for (int i = 0; i < parameters.length; i++) {
            sb.append(parameters[i]);
            if (i < parameters.length - 1) {
                sb.append(", ");
            }
        }

        sb.append(")");

        return sb.toString();
    }
}
