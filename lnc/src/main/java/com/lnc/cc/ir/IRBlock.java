package com.lnc.cc.ir;


import java.util.*;

public class IRBlock{

    private final IRUnit unit;

    private final Set<ILabelReferenceHolder> references = new HashSet<>();

    private final int id;

    private final LinkedList<IRInstruction> instructions = new LinkedList<>();

    private final List<IRBlock> predecessors = new ArrayList<>();
    private final List<IRBlock> successors = new ArrayList<>();

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

    public List<IRBlock> getSuccessors() {
        return successors;
    }

    public ListIterator<IRInstruction> listIterator() {
        return instructions.listIterator();
    }
}
