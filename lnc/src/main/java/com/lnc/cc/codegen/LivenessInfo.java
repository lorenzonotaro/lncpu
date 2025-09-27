package com.lnc.cc.codegen;

import com.lnc.cc.ir.IRBlock;
import com.lnc.cc.ir.IRInstruction;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

import java.util.*;

/**
 * Represents the liveness information of virtual registers within blocks of an intermediate representation (IR).
 * The liveness information includes two sets for each IR block:
 * - liveIn: A set of virtual registers that are live at the entry of the block.
 * - liveOut: A set of virtual registers that are live at the exit of the block.
 *
 * This record is useful for analyzing liveness during compiler optimization and register allocation.
 *
 * @param liveIn A mapping of each IR block to the set of virtual registers live at its entry.
 * @param liveOut A mapping of each IR block to the set of virtual registers live at its exit.
 */
public record LivenessInfo(
        Map<IRBlock, Set<VirtualRegister>> liveIn,
        Map<IRBlock, Set<VirtualRegister>> liveOut
) {
    public static LivenessInfo computeBlockLiveness(IRUnit unit) {
        List<IRBlock> blocks = unit.computeReversePostOrderAndCFG();
        if (blocks.isEmpty()) {
            return new LivenessInfo(Map.of(), Map.of());
        }
        IRBlock entry = blocks.get(0);

        // 1) Precompute uses[B] and defs[B] for each block
        Map<IRBlock, Set<VirtualRegister>> uses  = new LinkedHashMap<>();
        Map<IRBlock, Set<VirtualRegister>> defs  = new LinkedHashMap<>();
        for (IRBlock B : blocks) {
            Set<VirtualRegister> useSet = new LinkedHashSet<>();
            Set<VirtualRegister> defSet = new LinkedHashSet<>();

            for (IRInstruction inst = B.getFirst(); inst != null; inst = inst.getNext()) {
                // first account for uses before the current instruction's defs
                for (IROperand op : inst.getReads()) {
                    if (op instanceof VirtualRegister vr && !defSet.contains(vr)) {
                        useSet.add(vr);
                    }
                }
                // then record defs produced by this instruction
                for (IROperand op : inst.getWrites()) {
                    if (op instanceof VirtualRegister vr) {
                        defSet.add(vr);
                    }
                }
            }

            uses.put(B, useSet);
            defs.put(B, defSet);
        }

        // 3) Initialize liveIn/Out to empty sets
        Map<IRBlock, Set<VirtualRegister>> liveIn  = new LinkedHashMap<>();
        Map<IRBlock, Set<VirtualRegister>> liveOut = new LinkedHashMap<>();
        for (IRBlock B : blocks) {
            liveIn.put(B,  new LinkedHashSet<>());
            liveOut.put(B, new LinkedHashSet<>());
        }

        // 4) Fixed‐point iteration of
        //    liveOut[B] = ∪ liveIn[S] for S ∈ succ(B)
        //    liveIn[B]  = uses[B] ∪ (liveOut[B] – defs[B])
        boolean changed;
        do {
            changed = false;
            // process in reverse RPO for faster convergence (but any order works)
            for (int bi = blocks.size() - 1; bi >= 0; bi--) {
                IRBlock B = blocks.get(bi);

                // compute new liveOut
                Set<VirtualRegister> newOut = new LinkedHashSet<>();
                for (IRBlock succ : B.getSuccessors()) {
                    newOut.addAll(liveIn.get(succ));
                }

                // compute new liveIn
                Set<VirtualRegister> newIn = new LinkedHashSet<>(uses.get(B));
                for (VirtualRegister vr : newOut) {
                    if (!defs.get(B).contains(vr)) {
                        newIn.add(vr);
                    }
                }

                // check for changes
                if (!newOut.equals(liveOut.get(B))) {
                    liveOut.put(B, newOut);
                    changed = true;
                }
                if (!newIn.equals(liveIn.get(B))) {
                    liveIn.put(B, newIn);
                    changed = true;
                }
            }
        } while (changed);

        return new LivenessInfo(liveIn, liveOut);
    }
}
