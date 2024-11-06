package com.lnc.cc.ir;

import java.util.HashSet;
import java.util.Set;

public abstract class BranchingIRVisitor implements IIRVisitor<Void> {

    private final Set<IRBlock> visitedBlocks;

    protected BranchingIRVisitor() {
        visitedBlocks = new HashSet<>();
    }

    @Override
    public Void accept(Jle jle) {

        visit(jle.getNonTakenBranch());

        if(jle.getContinueTo() != null)
            append(new Goto(jle.getContinueTo()));

        visit(jle.getTarget());

        if(jle.getContinueTo() != null)
            visit(jle.getContinueTo());


        return null;
    }

    @Override
    public Void accept(Jeq je) {

        visit(je.getNonTakenBranch());

        if(je.getContinueTo() != null)
            append(new Goto(je.getContinueTo()));

        visit(je.getTarget());

        if(je.getContinueTo() != null)
            visit(je.getContinueTo());

        return null;
    }

    @Override
    public Void accept(Jlt jle) {

        visit(jle.getNonTakenBranch());

        if(jle.getContinueTo() != null)
            append(new Goto(jle.getContinueTo()));

        visit(jle.getTarget());

        if(jle.getContinueTo() != null)
            visit(jle.getContinueTo());

        return null;
    }

    protected void visit(IRBlock block) {

        if(block == null) {
            return;
        }
        
        if(visitedBlocks.contains(block)) {
            return;
        }

        visitedBlocks.add(block);

        for (IRInstruction instruction : block.getInstructions()) {
            instruction.accept(this);
        }
        

        if(block.hasNext()) {
            visit(block.getNext());
        }
    }

    protected abstract void append(IRInstruction aGoto);
}
