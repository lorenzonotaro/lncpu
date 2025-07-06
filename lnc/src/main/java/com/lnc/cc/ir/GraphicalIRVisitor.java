package com.lnc.cc.ir;

import java.util.*;

public abstract class GraphicalIRVisitor<T> implements IIRInstructionVisitor<T> {

    private final Set<IRBlock> visitedBlocks;

    private IRBlock currentBlock;

    private ListIterator<IRInstruction> instructionIterator;


    protected GraphicalIRVisitor() {
        visitedBlocks = new HashSet<>();
    }

    protected abstract T visit(IRBlock block);

    private T checkNotVisitedAndVisit(IRBlock block) {

        if(block == null || isVisited(block)) {
            return null;
        }

        visitedBlocks.add(block);

        currentBlock = block;

        this.visit(block);

        for (IRBlock successor : block.getSuccessors()) {
            checkNotVisitedAndVisit(successor);
        }

        return null;
    }

    public void visit(IRUnit unit){
        reset();
        for (IRBlock block : unit) {
            checkNotVisitedAndVisit(block);
        }
    }

    protected void reset() {
        visitedBlocks.clear();
        currentBlock = null;
    }

    protected boolean isVisited(IRBlock block) {
        return visitedBlocks.contains(block);
    }

    protected IRBlock getCurrentBlock() {
        return currentBlock;
    }
}
