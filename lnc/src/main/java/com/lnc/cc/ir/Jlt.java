package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

public class Jlt extends AbstractCJump {

    public Jlt(IROperand left, IROperand right, IRBlock target, IRBlock nonTakenBranch) {
        super(left, right, target, nonTakenBranch);
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }

    @Override
    public String toString() {
        return "jlt " + left + ", " + right + " -> " + getTarget();
    }
}
