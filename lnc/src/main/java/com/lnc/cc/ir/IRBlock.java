package com.lnc.cc.ir;


import java.util.ArrayList;
import java.util.List;

public class IRBlock {

    private final IRUnit unit;

    private IRBlock next;

    private final int id;

    private final List<IRInstruction> instructions = new ArrayList<>();

    public IRBlock(IRUnit unit, int id) {
        this.unit = unit;
        this.id = id;
    }


    public void emit(IRInstruction instruction) {
        instructions.add(instruction);
    }

    public int getId() {
        return id;
    }

    public List<IRInstruction> getInstructions() {
        return instructions;
    }

    public void setNext(IRBlock next) {
        this.next = next;
    }

    public boolean hasNext() {
        return next != null;
    }

    public IRBlock getNext() {
        return next;
    }



    @Override
    public String toString() {
        return "S" + id;
    }
}
