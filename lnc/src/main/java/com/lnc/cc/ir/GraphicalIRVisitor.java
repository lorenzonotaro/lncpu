package com.lnc.cc.ir;

import java.util.*;

public abstract class GraphicalIRVisitor implements IIRInstructionVisitor<Void> {

    private final Set<IRBlock> visitedBlocks;

    private IRBlock currentBlock;

    private ListIterator<IRInstruction> instructionIterator;


    protected GraphicalIRVisitor() {
        visitedBlocks = new HashSet<>();
    }

    protected boolean visit(IRBlock block) {

        if(block == null || isVisited(block)) {
            return false;
        }

        visitedBlocks.add(block);

        currentBlock = block;

        for (ListIterator<IRInstruction> it = block.listIterator(); it.hasNext(); ) {
            IRInstruction instruction = it.next();
            instruction.accept(this);
        }

        for (IRBlock successor : block.getSuccessors()) {
            visit(successor);
        }

        return true;
    }

    public void visit(IRUnit unit){
        reset();
        for (IRBlock block : unit) {
            visit(block);
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
