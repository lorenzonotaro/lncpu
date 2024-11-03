package com.lnc.cc.ir;

public abstract class AbstractBranchInstr extends IRInstruction {
    protected final IRBlock target;

    protected AbstractBranchInstr(IRBlock target) {
        this.target = target;
        target.addReference(this);
    }

    public IRBlock getTarget() {
        return target;
    }
}
