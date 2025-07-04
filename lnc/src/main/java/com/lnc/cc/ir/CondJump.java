package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public class CondJump extends AbstractBranchInstr {
    enum Cond { EQ, NE, LT, LE, GT, GE }
    private Cond         cond;           // the high-level relation
    private IROperand    left, right;    // what to compare
    private IRBlock      falseTarget;    // else

    public CondJump(Cond cond, IROperand left, IROperand right, IRBlock target, IRBlock falseTarget) {
        super(target);
        this.cond = cond;
        this.left = left;
        this.right = right;
        this.falseTarget = falseTarget;

    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.accept(this);
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
