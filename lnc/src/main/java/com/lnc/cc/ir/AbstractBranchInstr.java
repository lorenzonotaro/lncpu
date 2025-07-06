package com.lnc.cc.ir;

import java.util.Collection;

public abstract class AbstractBranchInstr extends IRInstruction {
    protected IRBlock target;

    protected AbstractBranchInstr(IRBlock target) {
        this.target = target;
    }

    public IRBlock getTarget() {
        return target;
    }

    public abstract void replaceReference(IRBlock block, IRBlock newBlock);

    public abstract Collection<? extends IRBlock> getSuccessors();
}
