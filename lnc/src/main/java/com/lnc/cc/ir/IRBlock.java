package com.lnc.cc.ir;


import java.util.*;

public class IRBlock {

    private final IRUnit unit;

    private List<AbstractBranchInstr> references = new ArrayList<>();

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

    public void addReference(AbstractBranchInstr instr) {
        references.add(instr);
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
        this.next = next;
    }

    public boolean hasNext() {
        return next != null;
    }
}
