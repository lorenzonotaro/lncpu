package com.lnc.cc.ir;

public abstract class AbstractBranchInstr extends IRInstruction implements ILabelReferenceHolder {
    protected IRBlock target;

    protected AbstractBranchInstr(IRBlock target) {
        this.target = target;
        target.addReference(this);
    }

    public IRBlock getTarget() {
        return target;
    }

    @Override
    public void onRemove() {
        target.removeReference(this);
    }

    @Override
    public void replaceReference(IRBlock block, IRBlock newBlock) {
        if(target.equals(block)){
            target.removeReference(this);

            target = newBlock;

            target.addReference(this);
        }
    }
}
