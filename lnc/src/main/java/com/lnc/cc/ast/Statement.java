package com.lnc.cc.ast;

public abstract class Statement {

    public final Type type;

    public Statement(Type type){
        this.type = type;
    }

    public enum Type{
        DECLARATION,
        IF,
        WHILE,
        FOR,
        RETURN,
        EXPRESSION_STMT,
        BLOCK,
        BREAK,
        CONTINUE
    }
}
