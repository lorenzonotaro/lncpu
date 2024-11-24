package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

public class Jle extends AbstractCJump {

    public Jle(IROperand left, IROperand right, IRBlock target, IRBlock nonTakenBranch) {
        super(left, right, target, nonTakenBranch);
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }

    @Override
    public String toString() {
        return "jle " + left + ", " + right + " -> " + getTarget();
    }
}
