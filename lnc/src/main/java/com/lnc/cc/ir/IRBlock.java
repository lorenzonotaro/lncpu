package com.lnc.cc.ir;


import com.lnc.common.Logger;

import java.util.*;

public class IRBlock {

    private final IRUnit unit;

    private Set<ILabelReferenceHolder> references = new HashSet<>();

    private final int id;

    private final LinkedList<IRInstruction> instructions = new LinkedList<>();

    private Set<IRBlock> predecessors = new HashSet<>();
    private Set<IRBlock> successors = new HashSet<>();

    public IRBlock(IRUnit unit, int id) {
        this.unit = unit;
        this.id = id;
    }


    public void emit(IRInstruction instruction) {

        if(!instructions.isEmpty() && instructions.getLast() instanceof AbstractBranchInstr && !(instruction instanceof AbstractBranchInstr)){
            throw new IllegalStateException("IR integrity alert: branch instruction must be the last instruction in a block");
        }

        instructions.add(instruction);
    }

    public int getId() {
        return id;
    }

    public List<IRInstruction> getInstructions() {
        return instructions;
    }

    @Override
    public String toString() {
        return "_l" + id;
    }

    public void addReference(ILabelReferenceHolder instr) {
        references.add(instr);
    }

    public void removeReference(ILabelReferenceHolder instr) {
        references.remove(instr);
    }

    public Collection<ILabelReferenceHolder> getReferences() {
        return references;
    }

    public void addSuccessor(IRBlock block) {
        this.successors.add(block);
    }

    public Set<IRBlock> getSuccessors() {
        return successors;
    }
}
