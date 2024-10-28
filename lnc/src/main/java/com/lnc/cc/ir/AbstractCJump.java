package com.lnc.cc.ir;

public abstract class AbstractCJump extends AbstractBranchInstr {
    public final IROperand left;
    public final IROperand right;

    public final IRBlock fallThrough;
    public final IRBlock continueTo;

    public AbstractCJump(IROperand left, IROperand right, IRBlock target, IRBlock fallThrough, IRBlock continueTo) {
        super(target);
        this.left = left;
        this.right = right;
        this.fallThrough = fallThrough;
        this.continueTo = continueTo;
    }

    public IRBlock getFallThrough() {
        return fallThrough;
    }

    public IRBlock getContinueTo() {
        return continueTo;
    }

    @Override
    public abstract <E> E accept(IIRVisitor<E> visitor);
}
