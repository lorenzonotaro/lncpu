package com.lnc.cc.ir;

import com.lnc.cc.ast.BinaryExpression;

public class Bin extends IRInstruction {
    private final IROperand left;
    private final IROperand right;
    private final BinaryExpression.Operator operator;

    public Bin(IROperand left, IROperand right, BinaryExpression.Operator operator) {
        super();
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public String toString() {
        return String.format("%s %s, %s", this.operator.toString().toLowerCase(), this.left, this.right);
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
