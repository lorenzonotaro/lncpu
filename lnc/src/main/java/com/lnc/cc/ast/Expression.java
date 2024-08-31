package com.lnc.cc.ast;

public abstract class Expression {

    public final Type type;

    public Expression(Type type){
        this.type = type;
    }

    public enum Type{
        UNARY, BINARY, MEMBER_ACCESS, SUBSCRIPT, CALL, IDENTIFIER, NUMERICAL, STRING, ASSIGNMENT

    }
}
