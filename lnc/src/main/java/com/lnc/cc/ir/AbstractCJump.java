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

        if(left.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)left).checkReleased();
        }

        if(right.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)right).checkReleased();
        }

        if(left instanceof ReferencableIROperand rop){
            rop.addRead(this);
        }

        if(right instanceof ReferencableIROperand rop) {
            rop.addRead(this);
        }

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
