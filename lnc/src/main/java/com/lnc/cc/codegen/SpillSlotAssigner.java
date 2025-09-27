package com.lnc.cc.codegen;

import com.lnc.cc.ir.Move;
import com.lnc.cc.ir.operands.StackFrameOperand;
import com.lnc.cc.ir.operands.VirtualRegister;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A utility class that assigns memory spill slots to virtual registers based on their lifetimes.
 *
 * The primary purpose of this class is to manage and allocate stack memory slots
 * for virtual registers when they need to be spilled (i.e., when there are not
 * enough physical registers available). It ensures that spills are efficiently
 * assigned by reusing memory slots where possible and recording the total number
 * of slots consumed.
 */
public class SpillSlotAssigner {
    private record SpillInfo(int start, int end, VirtualRegister vr, int sizeSlots) {}

    /** Final map: vreg → (slotOffset in *slots*) */
    public final Map<VirtualRegister,Integer> slotOffset = new HashMap<>();

    /** Total slots used at the end of assignSlots */
    private int totalSlots = 0;

    /**
     * Assigns spills and remembers the final slot count.
     */
    public void assignSlots(Map<VirtualRegister, LiveRange> liveRanges) {
        var slotSizeOf = liveRanges.keySet().stream()
            .collect(Collectors.toMap(vr -> vr, vr -> vr.getTypeSpecifier().allocSize()));

        List<SpillInfo> spills = liveRanges.entrySet().stream()
            .map(e -> new SpillInfo(e.getValue().start(), e.getValue().end(),
                                    e.getKey(), slotSizeOf.get(e.getKey())))
            .sorted(Comparator.comparingInt(SpillInfo::start))
            .toList();

        PriorityQueue<SpillInfo> active =
            new PriorityQueue<>(Comparator.comparingInt(SpillInfo::end));
        List<AbstractMap.SimpleEntry<Integer,Integer>> freeList = new ArrayList<>();
        int nextOffset = this.totalSlots;

        for (SpillInfo si : spills) {
            while (!active.isEmpty() && active.peek().end() <= si.start()) {
                var old = active.poll();
                int off = slotOffset.get(old.vr());
                freeList.add(new AbstractMap.SimpleEntry<>(off, old.sizeSlots()));
            }

            AbstractMap.SimpleEntry<Integer,Integer> chosenHole = null;
            for (var hole : freeList) {
                if (hole.getValue() >= si.sizeSlots()) {
                    chosenHole = hole;
                    break;
                }
            }

            int assignedOff;
            if (chosenHole != null) {
                assignedOff = chosenHole.getKey();
                if (chosenHole.getValue() > si.sizeSlots()) {
                    freeList.add(new AbstractMap.SimpleEntry<>(
                        assignedOff + si.sizeSlots(),
                        chosenHole.getValue() - si.sizeSlots()));
                }
                freeList.remove(chosenHole);
            } else {
                assignedOff = nextOffset;
                nextOffset += si.sizeSlots();
            }

            slotOffset.put(si.vr(), assignedOff);
            active.add(si);
        }

        // remember the final number of slots used
        this.totalSlots = nextOffset;
    }

    /** @return total number of slots consumed by all spills */
    public int getTotalSlots() {
        return totalSlots;
    }
}
