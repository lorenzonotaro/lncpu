package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

public class CondJump extends AbstractBranchInstr {
    enum Cond { EQ, NE, LT, LE, GT, GE }
    private final Cond         cond;           // the high-level relation
    private final IROperand    left;
    private final IROperand right;    // what to compare
    private final IRBlock      falseTarget;    // else

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

    @Override
    public String toString() {
        return "j" + cond.toString().toLowerCase() +
                " " + left +
                " " + right +
                ": " + target.toString() +
                " else: " + falseTarget.toString();
    }
}
