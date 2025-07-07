package com.lnc.cc.ast;

import com.lnc.cc.types.Declarator;
import com.lnc.common.frontend.Token;



public class VariableDeclaration extends Declaration {

    public final AssignmentExpression initializer;
    public final Declarator declarator;

    private final int parameterIndex;
    public boolean isParameter;

    public VariableDeclaration(Declarator declarator, Token name, AssignmentExpression initializer, boolean isParameter, int parameterIndex) {
        super(Declaration.Type.VARIABLE, name);
        this.declarator = declarator;
        this.initializer = initializer;
        this.isParameter = isParameter;
        this.parameterIndex = parameterIndex;
    }

    public VariableDeclaration(Declarator declarator, Token name, AssignmentExpression initializer) {
        this(declarator, name, initializer, false, -1);
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.visit(this);
    }

    public int getParameterIndex() {
        return parameterIndex;
    }
}
