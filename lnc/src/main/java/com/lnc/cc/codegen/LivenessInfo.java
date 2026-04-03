package com.lnc.cc.codegen;

import com.lnc.cc.ir.IRBlock;
import com.lnc.cc.ir.IRInstruction;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.ir.Bin;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.ast.BinaryExpression;

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
        Map<IRBlock, Set<VirtualRegister>> liveOut,
        Map<IRInstruction, Set<VirtualRegister>> instructionLiveAfter
) {
    private static Set<VirtualRegister> virtualRegisters(Collection<? extends IROperand> operands) {
        Set<VirtualRegister> out = new LinkedHashSet<>();
        for (IROperand op : operands) {
            if (op instanceof VirtualRegister vr) {
                out.add(vr);
            }
        }
        return out;
    }

    public static LivenessInfo computeBlockLiveness(IRUnit unit) {
        List<IRBlock> blocks = unit.computeReversePostOrderAndCFG();
        if (blocks.isEmpty()) {
            return new LivenessInfo(Map.of(), Map.of(), Map.of());
        }

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

        Map<IRInstruction, Set<VirtualRegister>> instructionLiveAfter =
                computeInstructionLiveAfter(unit, liveOut);

        return new LivenessInfo(liveIn, liveOut, instructionLiveAfter);
    }

    public static Map<IRInstruction, Set<VirtualRegister>> computeInstructionLiveAfter(
            IRUnit unit,
            Map<IRBlock, Set<VirtualRegister>> liveOut
    ) {
        Map<IRInstruction, Set<VirtualRegister>> liveAfterByInstruction = new LinkedHashMap<>();
        List<IRBlock> blocks = unit.computeReversePostOrderAndCFG();

        for (IRBlock block : blocks) {
            Set<VirtualRegister> live = new LinkedHashSet<>(
                    liveOut.getOrDefault(block, Collections.emptySet())
            );

            for (IRInstruction inst = block.getLast(); inst != null; inst = inst.getPrev()) {
                liveAfterByInstruction.put(inst, new LinkedHashSet<>(live));

                Set<VirtualRegister> defsHere = virtualRegisters(inst.getWrites());
                Set<VirtualRegister> usesHere = virtualRegisters(inst.getReads());
                live.removeAll(defsHere);
                live.addAll(usesHere);
            }
        }

        return liveAfterByInstruction;
    }

    public record DefUseStats(
            int writeCount,
            int readCount,
            int directReadCount,
            IRInstruction singleWriter
    ) {
        public boolean hasSingleWrite() {
            return writeCount == 1 && singleWriter != null;
        }

        public boolean allReadsAreDirect() {
            return readCount == directReadCount;
        }
    }

    public static DefUseStats computeDefUseStats(IRUnit unit, VirtualRegister vr) {
        int writeCount = 0;
        int readCount = 0;
        int directReadCount = 0;
        IRInstruction singleWriter = null;

        List<IRBlock> blocks = unit.computeReversePostOrderAndCFG();
        for (IRBlock block : blocks) {
            for (IRInstruction inst = block.getFirst(); inst != null; inst = inst.getNext()) {
                for (VirtualRegister readVr : inst.getReads()) {
                    if (vr.equals(readVr)) {
                        readCount++;
                    }
                }

                for (IROperand readOperand : inst.getReadOperands()) {
                    if (vr.equals(readOperand)) {
                        directReadCount++;
                    }
                }

                for (VirtualRegister writeVr : inst.getWrites()) {
                    if (vr.equals(writeVr)) {
                        writeCount++;
                        if (writeCount == 1) {
                            singleWriter = inst;
                        } else {
                            singleWriter = null;
                        }
                    }
                }
            }
        }

        return new DefUseStats(writeCount, readCount, directReadCount, singleWriter);
    }

    public static List<IRInstruction> directReadUsers(IRUnit unit, VirtualRegister vr) {
        List<IRInstruction> users = new ArrayList<>();
        List<IRBlock> blocks = unit.computeReversePostOrderAndCFG();

        for (IRBlock block : blocks) {
            for (IRInstruction inst = block.getFirst(); inst != null; inst = inst.getNext()) {
                for (IROperand readOperand : inst.getReadOperands()) {
                    if (vr.equals(readOperand)) {
                        users.add(inst);
                        break;
                    }
                }
            }
        }

        return users;
    }

    public static boolean canReplaceDirectReadWithImmediate(IRInstruction inst, VirtualRegister vr) {
        if (!directlyReads(inst, vr)) {
            return false;
        }

        if (!(inst instanceof Bin bin)) {
            return true;
        }

        if (!isWordAddOrSub(bin)) {
            return true;
        }

        // Word add/sub lowering requires register RHS. Replacing a read in a position
        // that becomes RHS after two-address normalization is unsafe.
        if (vr.equals(bin.getRight())) {
            return false;
        }

        return !(bin.getOperator().isCommutative()
                && bin.getDest().equals(bin.getRight())
                && vr.equals(bin.getLeft()));
    }

    private static boolean directlyReads(IRInstruction inst, VirtualRegister vr) {
        for (IROperand readOperand : inst.getReadOperands()) {
            if (vr.equals(readOperand)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWordAddOrSub(Bin bin) {
        if (bin.getDest().getTypeSpecifier().typeSize() != 2) {
            return false;
        }

        return bin.getOperator() == BinaryExpression.Operator.ADD
                || bin.getOperator() == BinaryExpression.Operator.SUB;
    }

    public Set<VirtualRegister> getLiveAfter(IRInstruction instr) {
        return instructionLiveAfter.getOrDefault(instr, Collections.emptySet());
    }

    public boolean isLiveAfter(VirtualRegister vr, IRInstruction instr) {
        if (instructionLiveAfter.containsKey(instr)) {
            return instructionLiveAfter.get(instr).contains(vr);
        }

        // Conservative fallback if this instruction is unknown to the map.
        for (IRInstruction p = instr.getNext(); p != null; p = p.getNext()) {
            if (p.getReads().contains(vr)) return true;
            if (p.getWrites().contains(vr)) return false;
        }
        return liveOut.getOrDefault(instr.getParentBlock(), Collections.emptySet()).contains(vr);
    }

    public boolean isDeadAfter(
            VirtualRegister vr,
            IRInstruction instr,
            Map<IRInstruction, Set<VirtualRegister>> liveAfterByInstruction
    ) {
        return !liveAfterByInstruction.getOrDefault(instr, Collections.emptySet()).contains(vr);
    }

    public boolean isDeadAfter(VirtualRegister vr, IRInstruction instr) {
        return !isLiveAfter(vr, instr);
    }
}
