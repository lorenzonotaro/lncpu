package com.lnc.cc.ir;

public class Goto extends AbstractBranchInstr {

    public Goto(IRBlock target) {
        super(target);
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
