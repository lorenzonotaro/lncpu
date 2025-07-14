package com.lnc.cc.ir;

import com.lnc.cc.ast.BinaryExpression;
import com.lnc.cc.ir.operands.IROperand;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Bin extends IRInstruction {
    private IROperand dest;

    public IROperand left;
    public IROperand right;
    private final BinaryExpression.Operator operator;
    public Bin(IROperand dest, IROperand left, IROperand right, BinaryExpression.Operator operator) {
        super();
        this.dest = dest;
        this.left = left;
        this.right = right;
        this.operator = operator;

        if(operator != BinaryExpression.Operator.ADD && operator != BinaryExpression.Operator.SUB && operator != BinaryExpression.Operator.AND && operator != BinaryExpression.Operator.OR && operator != BinaryExpression.Operator.XOR){
            throw new RuntimeException("invalid operator for Bin: %s".formatted(operator));
        }

    }

    @Override
    public String toString() {
        return String.format("%s <- %s %s, %s", this.dest, this.operator.toString(), this.left, this.right);
    }

    @Override
    public Collection<IROperand> getReadOperands() {
        return List.of(left, right);
    }

    @Override
    public Collection<IROperand> getWriteOperands() {
        return Collections.singleton(dest);
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (left.equals(oldOp)) {
            left = newOp;
        } else if (right.equals(oldOp)) {
            right = newOp;
        } else if (dest.equals(oldOp)) {
            dest = newOp;
        }
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    public IROperand getDest() {
        return dest;
    }

    public void setDest(IROperand dest) {
        this.dest = dest;
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

    public void swapOperands() {
        IROperand temp = left;
        left = right;
        right = temp;
    }
}
