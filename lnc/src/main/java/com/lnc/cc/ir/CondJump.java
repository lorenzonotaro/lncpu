package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.List;

public class CondJump extends AbstractBranchInstr {
    @Override
    public void replaceReference(IRBlock oldBlock, IRBlock newBlock) {
        if (target == oldBlock) {
            target = newBlock;
        }
        if (falseTarget == oldBlock) {
            falseTarget = newBlock;
        }
    }

    @Override
    public List<IRBlock> getTargets() {
        return List.of(target, falseTarget);
    }

    public Cond getCond() {
        return cond;
    }

    public void setLeft(IROperand right) {
        if (right == null) {
            throw new IllegalArgumentException("Left operand cannot be null");
        }
        this.left = right;
    }

    public void setRight(IROperand right) {
        if (right == null) {
            throw new IllegalArgumentException("Right operand cannot be null");
        }
        this.right = right;
    }


    public enum Cond { EQ, NE, LT, LE, GT, GE }
    private final Cond         cond;           // the high-level relation
    private IROperand    left;
    private IROperand right;    // what to compare
    private IRBlock      falseTarget;    // else

    public IRBlock getFalseTarget() {
        return falseTarget;
    }

    public CondJump(Cond cond, IROperand left, IROperand right, IRBlock target, IRBlock falseTarget) {
        super(target);
        this.cond = cond;
        this.left = left;
        this.right = right;
        this.falseTarget = falseTarget;

    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    public IROperand getLeft() {
        return left;
    }

    public IROperand getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "j" + cond.toString().toLowerCase() +
                " " + left +
                " " + right +
                ": " + target.toString() +
                " else: " + falseTarget.toString();
    }
}
