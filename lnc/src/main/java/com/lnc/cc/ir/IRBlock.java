package com.lnc.cc.ir;


import com.lnc.common.Logger;

import java.util.*;

public class IRBlock {

    private final IRUnit unit;

    private Set<ILabelReferenceHolder> references = new HashSet<>();

    private final int id;

    private final LinkedList<IRInstruction> instructions = new LinkedList<>();

    private IRBlock next;

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

    public IRBlock getNext() {
        return next;
    }

    public Set<IRBlock> getSuccessors() {
        SortedSet<IRBlock> successors = new TreeSet<>(Comparator.comparingInt(IRBlock::getId)); {
        };

        for(Iterator<IRInstruction> iterator = instructions.descendingIterator(); iterator.hasNext();){
            IRInstruction instruction = iterator.next();

            if(instruction instanceof AbstractBranchInstr){
                successors.add(((AbstractBranchInstr) instruction).getTarget());
            }else{
                break;
            }
        }

        if(next != null){
            successors.add(next);
        }

        return successors;
    }

    public void setNext(IRBlock next) {
        if(this.next != null)
            Logger.warning("overwriting next block %s with %s".formatted(this.next, next));
        this.next = next;
    }

    public boolean hasNext() {
        return next != null;
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
}
