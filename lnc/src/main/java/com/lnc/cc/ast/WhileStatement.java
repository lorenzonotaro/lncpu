package com.lnc.cc.ast;



public class WhileStatement extends Statement {

        public final Expression condition;
        public final Statement statement;

        public WhileStatement(Expression condition, Statement statement) {
            super(Statement.Type.WHILE);
            this.condition = condition;
            this.statement = statement;
        }

}
