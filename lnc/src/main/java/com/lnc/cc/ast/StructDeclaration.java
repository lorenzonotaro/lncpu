package com.lnc.cc.ast;

import com.lnc.cc.types.StructDefinitionType;
import com.lnc.common.frontend.Token;

public class StructDeclaration extends Declaration{

    private final StructDefinitionType structDefinition;

    public StructDeclaration(Token name, StructDefinitionType structDefinition) {
        super(Type.STRUCT, name);
        this.structDefinition = structDefinition;
    }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.visit(this);
    }

    public StructDefinitionType getStructDefinition() {
        return structDefinition;
    }
}
