package com.lnc.cc.ast;

public class BlockStatement extends Statement {

    public final Statement[] statements;

    public BlockStatement(Statement[] statements) {
        super(Statement.Type.BLOCK);
        this.statements = statements;
    }

}
