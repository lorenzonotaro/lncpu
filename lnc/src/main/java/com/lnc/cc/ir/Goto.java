package com.lnc.cc.ir;

import java.util.Collection;
import java.util.List;

public class Goto extends AbstractBranchInstr {

    public Goto(IRBlock target) {
        super(target);
    }

    @Override
    public void replaceReference(IRBlock block, IRBlock newBlock) {
        if (target == block) {
            target = newBlock;
        }
    }

    @Override
    public Collection<? extends IRBlock> getSuccessors() {
        return List.of(getTarget());
    }

    @Override
    public String toString(){
        return "goto " + getTarget();
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
