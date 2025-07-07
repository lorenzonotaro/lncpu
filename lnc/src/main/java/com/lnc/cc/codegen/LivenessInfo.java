package com.lnc.cc.codegen;

import com.lnc.cc.ir.IRBlock;
import com.lnc.cc.ir.IRInstruction;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

import java.util.*;

public record LivenessInfo(Map<IRBlock, Set<VirtualRegister>> liveIn, Map<IRBlock, Set<VirtualRegister>> liveOut) {
    public static LivenessInfo computeBlockLiveness(IRUnit unit) {
        List<IRBlock> blocks = unit.computeReversePostOrderAndCFG();

        // 1) Precompute uses[B] and defs[B] for each block
        Map<IRBlock, Set<VirtualRegister>> uses  = new HashMap<>();
        Map<IRBlock, Set<VirtualRegister>> defs  = new HashMap<>();
        for (IRBlock B : blocks) {
            Set<VirtualRegister> useSet = new LinkedHashSet<>();
            Set<VirtualRegister> defSet = new LinkedHashSet<>();
            for (IRInstruction inst = B.getFirst(); inst != null; inst = inst.getNext()) {
                // defs first: so that a read of something just defined in this block isn't counted as a use
                for (IROperand op : inst.getWrites()) {
                    if (op instanceof VirtualRegister vr) {
                        defSet.add(vr);
                    }
                }
                for (IROperand op : inst.getReads()) {
                    if (op instanceof VirtualRegister vr) {
                        if (!defSet.contains(vr)) {
                            useSet.add(vr);
                        }
                    }
                }
            }
            uses.put(B, useSet);
            defs.put(B, defSet);
        }

        // 2) Initialize liveIn/Out to empty
        Map<IRBlock, Set<VirtualRegister>> liveIn  = new HashMap<>();
        Map<IRBlock, Set<VirtualRegister>> liveOut = new HashMap<>();
        for (IRBlock B : blocks) {
            liveIn.put(B,  new LinkedHashSet<>());
            liveOut.put(B, new LinkedHashSet<>());
        }

        // 3) Iterate to fixed-point
        boolean changed;
        do {
            changed = false;
            // any block order works; RPO reversed often converges faster, but not required
            for (int bi = blocks.size() - 1; bi >= 0; bi--) {
                IRBlock B = blocks.get(bi);

                // compute new liveOut = union of liveIn of successors
                Set<VirtualRegister> newOut = new LinkedHashSet<>();
                for (IRBlock succ : B.getSuccessors()) {
                    newOut.addAll(liveIn.get(succ));
                }

                // compute new liveIn = uses[B] ∪ (newOut – defs[B])
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
