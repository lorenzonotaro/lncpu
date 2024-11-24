package com.lnc.cc.types;

import com.lnc.common.frontend.Token;

public class StructType extends TypeSpecifier{
    private final Token name;
    private final boolean providesDefinition;

    private StructDefinitionType definition;

    public StructType(Token name) {
        this(name, null);
    }

    public StructType(Token name, StructDefinitionType structDefinitionType) {
        super(Type.STRUCT, false);
        this.name = name;
        this.providesDefinition = structDefinitionType != null;
        this.definition = structDefinitionType;
    }

    public StructDefinitionType getDefinition() {
        return definition;
    }

    public boolean hasDefinition() {
        return definition != null;
    }

    public void bindDefinition(StructDefinitionType definition) {
        this.definition = definition;
    }

    @Override
    public int typeSize() {
        if(definition == null) throw new IllegalStateException("Struct type not defined");
        return definition.typeSize();
    }

    @Override
    public int allocSize() {
        if(definition == null) throw new IllegalStateException("Struct type not defined");
        return definition.allocSize();
    }

    public Token getName() {
        return name;
    }

    @Override
    public String toString() {
        return "struct " + name.lexeme;
    }

    public boolean providesDefinition() {
        return providesDefinition;
    }
}
