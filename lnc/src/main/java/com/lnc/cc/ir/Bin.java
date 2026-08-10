package com.lnc.cc.ir;

import com.lnc.cc.ast.BinaryExpression;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

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

        if(operator != BinaryExpression.Operator.ADD
                && operator != BinaryExpression.Operator.SUB
                && operator != BinaryExpression.Operator.AND
                && operator != BinaryExpression.Operator.OR
                && operator != BinaryExpression.Operator.XOR
                && operator != BinaryExpression.Operator.SHL
                && operator != BinaryExpression.Operator.SHR){
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

    /**
     * Code generation emits shifts in place on the left operand and copies the result out
     * afterwards ({@code shl left; mov left, dest}), so a shift clobbers its left operand as well as
     * its destination. Reporting that write keeps liveness, copy propagation and register allocation
     * from assuming the pre-shift value survives the instruction.
     */
    @Override
    public Collection<IROperand> getWriteOperands() {
        return clobbersLeftOperand() ? List.of(dest, left) : Collections.singleton(dest);
    }

    public boolean clobbersLeftOperand() {
        return (operator == BinaryExpression.Operator.SHL || operator == BinaryExpression.Operator.SHR)
                && left instanceof VirtualRegister
                && !left.equals(dest);
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        boolean replacedRead = false;
        if (left.equals(oldOp)) {
            left = newOp;
            replacedRead = true;
        }
        if (right.equals(oldOp)) {
            right = newOp;
            replacedRead = true;
        }
        if (!replacedRead && dest.equals(oldOp)) {
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
