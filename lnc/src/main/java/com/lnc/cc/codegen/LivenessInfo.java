package com.lnc.cc.codegen;

import com.lnc.cc.ir.IRBlock;
import com.lnc.cc.ir.IRInstruction;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

import java.util.*;

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
                // collect defs first
                for (IROperand op : inst.getWrites()) {
                    if (op instanceof VirtualRegister vr) {
                        defSet.add(vr);
                    }
                }
                // then uses, but skip those already in defSet
                for (IROperand op : inst.getReads()) {
                    if (op instanceof VirtualRegister vr && !defSet.contains(vr)) {
                        useSet.add(vr);
                    }
                }
            }

            uses.put(B, useSet);
            defs.put(B, defSet);
        }

        // 2) Implicitly define any register‐passed parameters in the entry block
        var paramMapping = unit.getFunctionType().getParameterMapping();
        for (int i = 0; i < paramMapping.size(); i++) {
            var loc = paramMapping.get(i);
            if (!loc.onStack() && loc.regClass() != null) {
                // find the VR in that class
                for (VirtualRegister vr : unit.getVirtualRegisterManager().getAllRegisters()) {
                    if (vr.getRegisterClass() == loc.regClass()) {
                        defs.get(entry).add(vr);
                    }
                }
            }
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
