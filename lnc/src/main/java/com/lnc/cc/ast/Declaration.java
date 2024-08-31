package com.lnc.cc.ast;

import com.lnc.cc.types.Declarator;
import com.lnc.cc.types.TypeQualifier;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

public abstract class Declaration extends Statement{

    public final Declaration.Type declarationType;
    public final Declarator declarator;

    public final Token name;

    public Declaration(Type declarationType, Declarator declarator, Token name) {
        super(Statement.Type.DECLARATION);
        this.declarationType = declarationType;
        this.declarator = declarator;
        this.name = name;
    }

    public enum Type {
        VARIABLE,
        FUNCTION
    }
}
