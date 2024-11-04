package com.lnc.cc.ir;

public class Jle extends AbstractCJump {

    public Jle(IROperand left, IROperand right, IRBlock target, IRBlock fallThrough, IRBlock continueTo) {
        super(left, right, target, fallThrough, continueTo);
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
