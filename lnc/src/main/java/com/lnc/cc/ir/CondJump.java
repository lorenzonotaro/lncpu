package com.lnc.cc.ir;

import com.lnc.cc.ast.BinaryExpression;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

import java.util.Collection;
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
        if (continueTo == oldBlock) {
            continueTo = newBlock;
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

    public IRBlock getContinueTo() {
        return continueTo;
    }

    public void setContinueTo(IRBlock continueTo) {
        this.continueTo = continueTo;
    }


    public enum Cond { EQ, NE, LT, LE, GT, GE;

        public static Cond of(BinaryExpression.Operator operator, Token token) {
            return switch (operator) {
                case EQ -> CondJump.Cond.EQ;
                case NE -> CondJump.Cond.NE;
                case LT -> CondJump.Cond.LT;
                case LE -> CondJump.Cond.LE;
                case GT -> CondJump.Cond.GT;
                case GE -> CondJump.Cond.GE;
                default -> throw new CompileException("unsupported comparison", token);
            };
        }

        public Cond inverse() {
            return switch (this) {
                case EQ -> NE;
                case NE -> EQ;
                case LT -> GE;
                case LE -> GT;
                case GT -> LE;
                case GE -> LT;
            };
        }
    }
    private final Cond         cond;           // the high-level relation
    private IROperand    left;
    private IROperand right;    // what to compare
    private IRBlock      falseTarget;    // else
    private IRBlock continueTo;

    public IRBlock getFalseTarget() {
        return falseTarget;
    }

    public CondJump(Cond cond, IROperand left, IROperand right, IRBlock target, IRBlock falseTarget, IRBlock continueTo) {
        super(target);
        this.cond = cond;
        this.left = left;
        this.right = right;
        this.falseTarget = falseTarget;
        this.continueTo = continueTo;
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

    @Override
    public Collection<IROperand> getReadOperands() {
        return List.of(left, right);
    }

    @Override
    public Collection<IROperand> getWriteOperands() {
        return List.of();
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (left.equals(oldOp)) {
            left = newOp;
        } else if (right.equals(oldOp)) {
            right = newOp;
        }
    }
}
