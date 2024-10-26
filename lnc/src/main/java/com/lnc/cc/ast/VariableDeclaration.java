package com.lnc.cc.ast;

import com.lnc.cc.types.Declarator;
import com.lnc.common.frontend.Token;



public class VariableDeclaration extends Declaration {

    public final AssignmentExpression initializer;

    public VariableDeclaration(Declarator declarator, Token name, AssignmentExpression initializer) {
        super(Declaration.Type.VARIABLE, declarator, name);
        this.initializer = initializer;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }
}
