package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.Collection;
import java.util.List;

public class Goto extends AbstractBranchInstr {

    public Goto(IRBlock target) {
        super(target);
    }

    @Override
    public void replaceReference(IRBlock oldBlock, IRBlock newBlock) {
        if (target == oldBlock) {
            target = newBlock;
        }
    }

    @Override
    public List<IRBlock> getTargets() {
        return List.of(getTarget());
    }

    @Override
    public String toString(){
        return "goto " + getTarget();
    }

    @Override
    public Collection<IROperand> getReads() {
        return List.of();
    }

    @Override
    public Collection<IROperand> getWrites() {
        return List.of();
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        // No operands to replace in Goto instruction
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
