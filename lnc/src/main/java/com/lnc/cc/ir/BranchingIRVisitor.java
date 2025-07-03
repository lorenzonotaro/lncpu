package com.lnc.cc.ir;

import java.util.*;

public abstract class BranchingIRVisitor implements IIRInstructionVisitor<Void> {

    private final Set<IRBlock> visitedBlocks;

    private IRBlock currentBlock;


    protected BranchingIRVisitor() {
        visitedBlocks = new HashSet<>();
    }

    protected boolean visit(IRBlock block) {

        if(block == null || isVisited(block)) {
            return false;
        }

        visitedBlocks.add(block);

        currentBlock = block;

        for (IRInstruction instruction : block.getInstructions()) {
            instruction.accept(this);
        }

        if(block.hasNext()) {
            visit(block.getNext());
        }

        return true;
    }

    protected boolean isVisited(IRBlock block) {
        return visitedBlocks.contains(block);
    }


    protected IRBlock getCurrentBlock() {
        return currentBlock;
    }
}
