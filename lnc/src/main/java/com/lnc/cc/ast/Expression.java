package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public abstract class Expression {

    public final Type type;
    public final Token token;

    public Expression(Type type, Token token){
        this.type = type;
        this.token = token;
    }

    public enum Type{
        UNARY, BINARY, MEMBER_ACCESS, SUBSCRIPT, CALL, IDENTIFIER, NUMERICAL, STRING, ASSIGNMENT

    }

    public abstract <E> E accept(IExpressionVisitor<E> visitor);
}
