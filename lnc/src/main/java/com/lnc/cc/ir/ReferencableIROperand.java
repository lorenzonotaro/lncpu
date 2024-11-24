package com.lnc.cc.ir;

import com.lnc.cc.codegen.LiveRange;
import com.lnc.cc.ir.operands.IROperand;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ReferencableIROperand extends IROperand {

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

    public LiveRange getLiveRange() {
        List<IRInstruction> list = allUses.stream().sorted(Comparator.comparingInt(IRInstruction::getIndex)).toList();
        return new LiveRange(list.get(0).getIndex(), list.get(list.size() - 1).getIndex());
    }
}
