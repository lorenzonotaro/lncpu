package com.lnc.cc.ir;

import java.util.HashSet;
import java.util.Set;

public class ReferencableIROperand extends IROperand {

    private final Set<IRInstruction> reads;
    private final Set<IRInstruction> writes;
    private final Set<IRInstruction> allUses;

    public ReferencableIROperand(Type type) {
        super(type);
        reads = new HashSet<>();
        writes = new HashSet<>();
        allUses = new HashSet<>();
    }

    public Set<IRInstruction> getReads() {
        return reads;
    }

    public Set<IRInstruction> getWrites() {
        return writes;
    }

    public void addRead(IRInstruction instruction) {
        reads.add(instruction);
        allUses.add(instruction);
    }

    public void addWrite(IRInstruction instruction) {
        writes.add(instruction);
        allUses.add(instruction);
    }

    public boolean removeRead(IRInstruction instruction) {
        return allUses.remove(instruction) | reads.remove(instruction);
    }

    public boolean removeWrite(IRInstruction instruction) {
        return allUses.remove(instruction) | writes.remove(instruction);
    }

    public int spillCost(){
        return allUses.stream().mapToInt(e -> Math.max(e.getLoopNestedLevel() * 10, 1)).sum();
    }

    public int promotionBenefit(){
        return spillCost();
    }
}
