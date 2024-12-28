package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public abstract class AbstractCJump extends AbstractBranchInstr {
    public final IROperand left;
    public final IROperand right;

    public final IRBlock nonTakenBranch;

    public AbstractCJump(IROperand left, IROperand right, IRBlock takenBranch, IRBlock nonTakenBranch) {
        super(takenBranch);
        this.left = left;
        this.right = right;
        this.nonTakenBranch = nonTakenBranch;

        if(left.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)left).checkReleased();
        }

        if(right.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)right).checkReleased();
        }

        if(left instanceof IReferenceable rop){
            rop.addRead(this);
        }

        if(right instanceof IReferenceable rop) {
            rop.addRead(this);
        }

    }

    public IRBlock getNonTakenBranch() {
        return nonTakenBranch;
    }

    @Override
    public abstract <E> E accept(IIRInstructionVisitor<E> visitor);
}
