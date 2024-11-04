package com.lnc.cc.ast;



public class WhileStatement extends Statement {

        public final Expression condition;
        public final Statement body;

        public WhileStatement(Expression condition, Statement body) {
            super(Statement.Type.WHILE);
            this.condition = condition;
            this.body = body;
        }

    @Override
    public <S> S accept(IStatementVisitor<S> visitor) {
        return visitor.accept(this);
    }
}
