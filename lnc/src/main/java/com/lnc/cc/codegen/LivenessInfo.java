package com.lnc.cc.codegen;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.SizedCast;
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
        Map<IRInstruction, Set<VirtualRegister>> instructionLiveAfter,
        Map<IRBlock, Map<VirtualRegister, Integer>> liveInMasks,
        Map<IRBlock, Map<VirtualRegister, Integer>> liveOutMasks,
        Map<IRInstruction, Map<VirtualRegister, Integer>> instructionLiveAfterMasks
) {
    public static final int COMPONENT_LOW = 0x1;
    public static final int COMPONENT_HIGH = 0x2;
    public static final int COMPONENT_BOTH = COMPONENT_LOW | COMPONENT_HIGH;

    private static int fullMask(VirtualRegister vr) {
        return vr.getTypeSpecifier().allocSize() == 2 ? COMPONENT_BOTH : COMPONENT_LOW;
    }

    private static void mergeMask(Map<VirtualRegister, Integer> map, VirtualRegister vr, int mask) {
        if (mask == 0) {
            return;
        }
        map.merge(vr, mask, (a, b) -> a | b);
    }

    private static void removeMask(Map<VirtualRegister, Integer> map, VirtualRegister vr, int mask) {
        if (mask == 0) {
            return;
        }
        int current = map.getOrDefault(vr, 0);
        int next = current & ~mask;
        if (next == 0) {
            map.remove(vr);
        } else if (next != current) {
            map.put(vr, next);
        }
    }

    private static Map<VirtualRegister, Integer> projectAnyLive(Map<VirtualRegister, Integer> masks) {
        Map<VirtualRegister, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<VirtualRegister, Integer> e : masks.entrySet()) {
            if (e.getValue() != 0) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private static Set<VirtualRegister> projectAnyLiveSet(Map<VirtualRegister, Integer> masks) {
        return new LinkedHashSet<>(projectAnyLive(masks).keySet());
    }

    private static Map<VirtualRegister, Integer> copyMaskMap(Map<VirtualRegister, Integer> source) {
        return new LinkedHashMap<>(projectAnyLive(source));
    }

    private static Map<VirtualRegister, Integer> computeReadMasksFromOperand(IROperand op) {
        Map<VirtualRegister, Integer> out = new LinkedHashMap<>();

        if (op instanceof VirtualRegister vr) {
            mergeMask(out, vr, fullMask(vr));
            return out;
        }

        if (op instanceof SizedCast sizedCast && sizedCast.getOperand() instanceof VirtualRegister vr) {
            int sourceSize = vr.getTypeSpecifier().allocSize();
            int targetSize = sizedCast.getTypeSpecifier().allocSize();

            if (sourceSize == 2 && targetSize == 1) {
                int mask = sizedCast.getByteSelection() == SizedCast.ByteSelection.HIGH
                        ? COMPONENT_HIGH
                        : COMPONENT_LOW;
                mergeMask(out, vr, mask);
                return out;
            }
        }

        for (VirtualRegister vr : op.getVRReads()) {
            mergeMask(out, vr, fullMask(vr));
        }
        return out;
    }

    public static Map<VirtualRegister, Integer> computeReadMasks(IRInstruction inst) {
        Map<VirtualRegister, Integer> out = new LinkedHashMap<>();

        for (IROperand readOperand : inst.getReadOperands()) {
            for (Map.Entry<VirtualRegister, Integer> e : computeReadMasksFromOperand(readOperand).entrySet()) {
                mergeMask(out, e.getKey(), e.getValue());
            }
        }

        // Keep parity with IRInstruction.getReads() for implicit reads coming from addressing operands.
        for (VirtualRegister vr : inst.getReads()) {
            mergeMask(out, vr, out.getOrDefault(vr, fullMask(vr)));
        }

        return out;
    }

    public static Map<VirtualRegister, Integer> computeWriteMasks(IRInstruction inst) {
        Map<VirtualRegister, Integer> out = new LinkedHashMap<>();

        for (VirtualRegister vr : inst.getWrites()) {
            mergeMask(out, vr, fullMask(vr));
        }

        return out;
    }


    public static LivenessInfo computeBlockLiveness(IRUnit unit) {
        List<IRBlock> blocks = unit.computeReversePostOrderAndCFG();
        if (blocks.isEmpty()) {
            return new LivenessInfo(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }

        // 1) Precompute component-aware uses[B] and defs[B] for each block
        Map<IRBlock, Map<VirtualRegister, Integer>> uses = new LinkedHashMap<>();
        Map<IRBlock, Map<VirtualRegister, Integer>> defs = new LinkedHashMap<>();
        for (IRBlock B : blocks) {
            Map<VirtualRegister, Integer> useMask = new LinkedHashMap<>();
            Map<VirtualRegister, Integer> defMask = new LinkedHashMap<>();

            for (IRInstruction inst = B.getFirst(); inst != null; inst = inst.getNext()) {
                Map<VirtualRegister, Integer> reads = computeReadMasks(inst);
                Map<VirtualRegister, Integer> writes = computeWriteMasks(inst);

                // Account for reads that happen before the corresponding component is defined in this block.
                for (Map.Entry<VirtualRegister, Integer> e : reads.entrySet()) {
                    VirtualRegister vr = e.getKey();
                    int unseen = e.getValue() & ~defMask.getOrDefault(vr, 0);
                    mergeMask(useMask, vr, unseen);
                }

                // Record newly-defined components.
                for (Map.Entry<VirtualRegister, Integer> e : writes.entrySet()) {
                    mergeMask(defMask, e.getKey(), e.getValue());
                }
            }

            uses.put(B, useMask);
            defs.put(B, defMask);
        }

        // 2) Initialize component-aware liveIn/Out to empty maps.
        Map<IRBlock, Map<VirtualRegister, Integer>> liveInMask = new LinkedHashMap<>();
        Map<IRBlock, Map<VirtualRegister, Integer>> liveOutMask = new LinkedHashMap<>();
        for (IRBlock B : blocks) {
            liveInMask.put(B, new LinkedHashMap<>());
            liveOutMask.put(B, new LinkedHashMap<>());
        }

        // 3) Fixed-point iteration:
        //    liveOut[B] = OR(liveIn[S]) for S in succ(B)
        //    liveIn[B]  = uses[B] OR (liveOut[B] - defs[B])
        boolean changed;
        do {
            changed = false;

            for (int bi = blocks.size() - 1; bi >= 0; bi--) {
                IRBlock B = blocks.get(bi);

                Map<VirtualRegister, Integer> newOut = new LinkedHashMap<>();
                for (IRBlock succ : B.getSuccessors()) {
                    for (Map.Entry<VirtualRegister, Integer> e : liveInMask.get(succ).entrySet()) {
                        mergeMask(newOut, e.getKey(), e.getValue());
                    }
                }

                Map<VirtualRegister, Integer> newIn = copyMaskMap(uses.get(B));
                for (Map.Entry<VirtualRegister, Integer> e : newOut.entrySet()) {
                    int surviving = e.getValue() & ~defs.get(B).getOrDefault(e.getKey(), 0);
                    mergeMask(newIn, e.getKey(), surviving);
                }

                if (!newOut.equals(liveOutMask.get(B))) {
                    liveOutMask.put(B, newOut);
                    changed = true;
                }
                if (!newIn.equals(liveInMask.get(B))) {
                    liveInMask.put(B, newIn);
                    changed = true;
                }
            }
        } while (changed);

        Map<IRInstruction, Map<VirtualRegister, Integer>> instructionLiveAfterMask =
                computeInstructionLiveAfter(unit, liveOutMask);

        Map<IRInstruction, Set<VirtualRegister>> instructionLiveAfter = new LinkedHashMap<>();
        for (Map.Entry<IRInstruction, Map<VirtualRegister, Integer>> e : instructionLiveAfterMask.entrySet()) {
            instructionLiveAfter.put(e.getKey(), projectAnyLiveSet(e.getValue()));
        }

        Map<IRBlock, Set<VirtualRegister>> liveIn = new LinkedHashMap<>();
        Map<IRBlock, Set<VirtualRegister>> liveOut = new LinkedHashMap<>();
        for (IRBlock block : blocks) {
            liveIn.put(block, projectAnyLiveSet(liveInMask.get(block)));
            liveOut.put(block, projectAnyLiveSet(liveOutMask.get(block)));
        }

        return new LivenessInfo(
                liveIn,
                liveOut,
                instructionLiveAfter,
                liveInMask,
                liveOutMask,
                instructionLiveAfterMask
        );
    }

    public static Map<IRInstruction, Map<VirtualRegister, Integer>> computeInstructionLiveAfter(
            IRUnit unit,
            Map<IRBlock, Map<VirtualRegister, Integer>> liveOutMasks
    ) {
        Map<IRInstruction, Map<VirtualRegister, Integer>> liveAfterByInstruction = new LinkedHashMap<>();
        List<IRBlock> blocks = unit.computeReversePostOrderAndCFG();

        for (IRBlock block : blocks) {
            Map<VirtualRegister, Integer> live = copyMaskMap(
                    liveOutMasks.getOrDefault(block, Collections.emptyMap())
            );

            for (IRInstruction inst = block.getLast(); inst != null; inst = inst.getPrev()) {
                liveAfterByInstruction.put(inst, copyMaskMap(live));

                Map<VirtualRegister, Integer> defsHere = computeWriteMasks(inst);
                Map<VirtualRegister, Integer> usesHere = computeReadMasks(inst);

                for (Map.Entry<VirtualRegister, Integer> e : defsHere.entrySet()) {
                    removeMask(live, e.getKey(), e.getValue());
                }
                for (Map.Entry<VirtualRegister, Integer> e : usesHere.entrySet()) {
                    mergeMask(live, e.getKey(), e.getValue());
                }
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

        if(inst instanceof Call){ // Calls never get direct reads
            return false;
        }

        if (!(inst instanceof Bin bin)) {
            return true;
        }

        // Shift lowering has operand-form constraints: the shifted value may need to stay
        // in a register, and the shift count must remain in its dedicated immediate/register
        // form. Replacing a direct read with an arbitrary immediate is therefore unsafe.
        if(bin.getOperator() == BinaryExpression.Operator.SHL || bin.getOperator() == BinaryExpression.Operator.SHR){
            return false;
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

    public Map<VirtualRegister, Integer> getLiveAfterMasks(IRInstruction instr) {
        return instructionLiveAfterMasks.getOrDefault(instr, Collections.emptyMap());
    }

    public int getLiveAfterMask(VirtualRegister vr, IRInstruction instr) {
        return getLiveAfterMasks(instr).getOrDefault(vr, 0);
    }

    public boolean isLiveAfter(VirtualRegister vr, IRInstruction instr) {
        if (instructionLiveAfterMasks.containsKey(instr)) {
            return instructionLiveAfterMasks.get(instr).getOrDefault(vr, 0) != 0;
        }

        // Conservative fallback if this instruction is unknown to the map.
        for (IRInstruction p = instr.getNext(); p != null; p = p.getNext()) {
            if (p.getReads().contains(vr)) return true;
            if (p.getWrites().contains(vr)) return false;
        }
        return liveOutMasks.getOrDefault(instr.getParentBlock(), Collections.emptyMap())
                .getOrDefault(vr, 0) != 0;
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
