package com.lnc.cc.ir;

import java.util.List;

public abstract class AbstractBranchInstr extends IRInstruction {
    protected IRBlock target;

    protected AbstractBranchInstr(IRBlock target) {
        this.target = target;
    }

    public IRBlock getTarget() {
        return target;
    }

    public abstract void replaceReference(IRBlock oldBlock, IRBlock newBlock);

    public abstract List<IRBlock> getTargets();
}
