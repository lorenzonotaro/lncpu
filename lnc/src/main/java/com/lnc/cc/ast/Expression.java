package com.lnc.cc.ast;

import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

public abstract class Expression {

    private final Type type;
    public final Token token;
    public TypeSpecifier typeSpecifier;

    public Expression(Type type, Token token){
        this.type = type;
        this.token = token;
    }

    public void setTypeSpecifier(TypeSpecifier typeSpecifier) {
        this.typeSpecifier = typeSpecifier;
    }

    public TypeSpecifier getTypeSpecifier() {

        if(typeSpecifier == null){
            throw new IllegalStateException("Type specifier is not set for expression: " + this);
        }

        return typeSpecifier;
    }

    public enum Type{
        UNARY, BINARY, MEMBER_ACCESS, SUBSCRIPT, CALL, IDENTIFIER, NUMERICAL, STRING, ASSIGNMENT

    }

    public abstract <E> E accept(IExpressionVisitor<E> visitor);
}
