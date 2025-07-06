package com.lnc.cc.ir;

import com.lnc.cc.ast.BinaryExpression;
import com.lnc.cc.ir.operands.IROperand;

public class Bin extends IRInstruction {
    private final IROperand target;

    public IROperand left;
    public IROperand right;
    private final BinaryExpression.Operator operator;
    public Bin(IROperand target, IROperand left, IROperand right, BinaryExpression.Operator operator) {
        super();
        this.target = target;
        this.left = left;
        this.right = right;
        this.operator = operator;

        if(operator != BinaryExpression.Operator.ADD && operator != BinaryExpression.Operator.SUB && operator != BinaryExpression.Operator.AND && operator != BinaryExpression.Operator.OR && operator != BinaryExpression.Operator.XOR){
            throw new RuntimeException("invalid operator for Bin: %s".formatted(operator));
        }

    }

    @Override
    public String toString() {
        return String.format("%s <- %s %s, %s", this.target, this.operator.toString(), this.left, this.right);
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    public IROperand getLeft() {
        return left;
    }

    public void setLeft(IROperand left) {
        this.left = left;
    }

    public IROperand getRight() {
        return right;
    }

    public void setRight(IROperand right) {
        this.right = right;
    }

    public BinaryExpression.Operator getOperator() {
        return operator;
    }
}
