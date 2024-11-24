package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public abstract class Declaration extends Statement{

    public final Declaration.Type declarationType;

    public final Token name;

    public Declaration(Type declarationType, Token name) {
        super(Statement.Type.DECLARATION);
        this.declarationType = declarationType;
        this.name = name;
    }

    public enum Type {
        VARIABLE,
        STRUCT,
        FUNCTION
    }
}
