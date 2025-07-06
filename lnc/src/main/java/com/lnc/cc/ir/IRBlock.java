package com.lnc.cc.ir;


import java.util.*;

public class IRBlock{

    private final IRUnit unit;

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

    public List<IRBlock> getSuccessors() {
        return successors;
    }

    public ListIterator<IRInstruction> listIterator() {
        return instructions.listIterator();
    }

    public void computeSuccessorsAndPredecessors() {
        successors.clear();
        predecessors.clear();

        // Validate that the last instruction is either a return or instanceof AbstractBranchInstr
        // and that no other instruction in the block other than the last one is a branch instruction

        if (instructions.isEmpty()) {
            return; // No instructions to process
        }

        IRInstruction lastInstruction = instructions.getLast();
        if (lastInstruction instanceof AbstractBranchInstr branch) {
            this.successors.addAll(branch.getSuccessors());
        }else if(!(lastInstruction instanceof Ret)) {
            throw new IllegalStateException("Last instruction in block must be a branch or return instruction: " + lastInstruction);
        }
    }

    public void updateReferences(IRBlock oldBlock, IRBlock newBlock){
        for (IRInstruction instruction : instructions) {
            if (instruction instanceof AbstractBranchInstr branch) {
                branch.replaceReference(oldBlock, newBlock);
            }
        }
    }

    public void replaceWith(IRBlock newBlock){
        for (IRBlock successor : successors) {
            successor.updateReferences(this, newBlock);
            successor.predecessors.remove(this);
        }
    }
}
