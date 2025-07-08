package com.lnc.cc.codegen;


import com.lnc.LNC;
import com.lnc.cc.ir.IRBlock;
import com.lnc.cc.ir.IRInstruction;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.ir.Move;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.StackFrameOperand;
import com.lnc.cc.ir.operands.VirtualRegister;

import java.util.*;
import java.util.stream.Collectors;

public class GraphColoringRegisterAllocator {
    private final InterferenceGraph graph;
    private final int K;  // total number of physical “colors”

    // worklists and stacks
    private final Deque<InterferenceGraph.Node> selectStack = new ArrayDeque<>();
    private final Set<InterferenceGraph.Node> spillCandidates = new LinkedHashSet<>();
    private final Set<InterferenceGraph.Node> coloredNodes    = new LinkedHashSet<>();
    private final Set<InterferenceGraph.Node> precoloredNodes = new LinkedHashSet<>();
    private final Set<Register> usedRegisters = new LinkedHashSet<>();

    public GraphColoringRegisterAllocator(InterferenceGraph graph) {
        this.graph = graph;
        // count physical nodes (excluding compounds if you treat them specially)
        this.K = graph.getPhysicalNodes().size();
        // separate out pre-colored
        for (var n : graph.getVirtualNodes()) {
            if(n.precolored != null){
                precoloredNodes.add(n);
            }
        }
    }

    public void allocate() {
        simplify();
        select();
        assignColors();
    }

    private void simplify() {
        // build mutable work graph only over non-precolored nodes
        var workGraph = new HashMap<InterferenceGraph.Node, Set<InterferenceGraph.Node>>();
        for (var n : graph.getVirtualNodes()) {
            if (precoloredNodes.contains(n)) continue;
            // only keep non-precolored neighbors
            Set<InterferenceGraph.Node> nbrs = n.adj.stream()
                    .filter(neighbor -> !precoloredNodes.contains(neighbor))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            workGraph.put(n, nbrs);
        }

        // removal loop unchanged
        while (!workGraph.isEmpty()) {
            var removable = workGraph.keySet().stream()
                    .filter(n -> workGraph.get(n).size() < n.allowedColors().size())
                    .min(Comparator.comparingInt(n -> n.allowedColors().size()));

            if (removable.isPresent()) {
                var node = removable.get();
                selectStack.push(node);
                for (var neighbor : workGraph.get(node)) {
                    workGraph.get(neighbor).remove(node);
                }
                workGraph.remove(node);
            } else {
                var spill = workGraph.keySet().stream()
                        .max(Comparator.comparingInt(n -> workGraph.get(n).size()))
                        .get();
                spillCandidates.add(spill);
                selectStack.push(spill);
                for (var neighbor : workGraph.get(spill)) {
                    workGraph.get(neighbor).remove(spill);
                }
                workGraph.remove(spill);
            }
        }
    }

    private void select() {
        // by the time simplify is done, selectStack has every VR node
        while (!selectStack.isEmpty()) {
            var node = selectStack.pop();

            // compute forbidden colors from already‐colored neighbors
            Set<Register> forbidden = new HashSet<>();
            for (var nbr : node.adj) {
                if (coloredNodes.contains(nbr) || precoloredNodes.contains(nbr)) {
                    forbidden.add(nbr.assigned);
                }
            }

            // pick an allowed color not forbidden
            Optional<Register> maybeColor = node.allowedColors().stream()
                    .filter(c -> !forbidden.contains(c))
                    .min(Comparator.comparingInt(Register::ordinal));

            if (maybeColor.isPresent()) {
                node.assigned = maybeColor.get();
                coloredNodes.add(node);
            } else {
                // real spill
                spillCandidates.add(node);
            }
        }
    }

    private void assignColors() {
        // precolored nodes already have assigned = precolored.get()
        // coloredNodes have their .assigned set
        // spillCandidates need spill code insertion downstream
        for (var n : precoloredNodes) {
            n.assigned = n.precolored;
            n.vr.setAssignedPhysicalRegister(n.precolored);
            this.usedRegisters.add(n.precolored);
        }

        for(var n : coloredNodes) {
            // Skip any that were also pre-colored
            if (n.precolored != null) continue;
            n.vr.setAssignedPhysicalRegister(n.assigned);
            this.usedRegisters.add(n.assigned);
        }

        // at this point, any node not in coloredNodes ∪ precoloredNodes is spilled
    }

    private Set<Register> getUsedRegisters() {
        return usedRegisters;
    }

    public Set<InterferenceGraph.Node> getSpills() {
        return spillCandidates;
    }


    public static void run(IRUnit unit){

        int maxIter = LNC.settings.get("--reg-alloc-max-iter", Double.class).intValue();

        List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillStores = new ArrayList<>();
        List<AbstractMap.SimpleEntry<VirtualRegister, Move>>  spillLoads  = new ArrayList<>();

        SpillSlotAssigner slotAssigner = new SpillSlotAssigner();

        Set<Register> usedRegisters = new LinkedHashSet<>();

        while (true) {

            if(maxIter-- <= 0) {
                throw new RuntimeException("Exceeded maximum iterations for register allocation.");
            }

            // 1) Build graph & run allocator
            InterferenceGraph ig = InterferenceGraph.buildInterferenceGraph(unit);

            GraphColoringRegisterAllocator allocator = new GraphColoringRegisterAllocator(ig);
            allocator.allocate();
            Set<InterferenceGraph.Node> spills = allocator.getSpills();

            usedRegisters = allocator.getUsedRegisters();


            if (spills.isEmpty()) {
                // no more spills ⇒ every vreg (including temps) has allocator-assigned registers
                break;
            }else{

                // 2) Insert spill code for each spilled vreg
                for (IRBlock bb : unit.computeReversePostOrderAndCFG()) {
                    for (IRInstruction inst = bb.getFirst(); inst != null; inst = inst.getNext()) {
                        // after defs
                        for (IROperand def : inst.getWrites()) {
                            if (def instanceof VirtualRegister vr && spills.contains(ig.getNode(vr))) {
                                Move move = new Move(vr, new StackFrameOperand(vr.getTypeSpecifier(), StackFrameOperand.OperandType.LOCAL, 0));
                                spillStores.add(new AbstractMap.SimpleEntry<>(vr, move));
                                inst.insertBefore(move);
                            }
                        }
                        // before uses
                        for (IROperand use : inst.getReads()) {
                            if (use instanceof VirtualRegister vr && spills.contains(ig.getNode(vr))) {
                                // allocate a temp for the loaded value
                                VirtualRegister temp = unit.getVirtualRegisterManager().getRegister(vr.getTypeSpecifier());
                                temp.setRegisterClass(vr.getRegisterClass());
                                Move load = new Move(new StackFrameOperand(vr.getTypeSpecifier(), StackFrameOperand.OperandType.LOCAL, 0), temp);
                                spillLoads.add(new AbstractMap.SimpleEntry<>(vr, load));
                                inst.insertBefore(load);
                                inst.replaceOperand(use, temp);
                            }
                        }
                    }
                }

                // 3) Assign concrete stack slots and patch offsets
                var livenessInfo = LivenessInfo.computeBlockLiveness(unit);

                Map<VirtualRegister, LiveRange> allRanges = InterferenceGraph.computeLiveRanges(unit, livenessInfo);
                Map<VirtualRegister, LiveRange> spillRanges = allRanges.entrySet().stream()
                        .filter(e -> spills.contains(ig.getNode(e.getKey())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                slotAssigner.assignSlots(spillRanges);
                patchSpillOffsets(spillStores, spillLoads, slotAssigner.slotOffset);
            }

            // loop back and re-allocate on the new IR (temps + spill code)
        }

        unit.setSpillSpaceSize(slotAssigner.getTotalSlots());
        unit.setUsedRegisters(usedRegisters);
    }

    private static void patchSpillOffsets(List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillStores, List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillLoads, Map<VirtualRegister, Integer> slotOf) {
        for(var entry : spillStores) {
            VirtualRegister vr = entry.getKey();
            Move store = entry.getValue();
            StackFrameOperand sfOp = (StackFrameOperand) store.getDest();
            sfOp.setOffset(slotOf.get(vr));
        }

        for(var entry : spillLoads) {
            VirtualRegister vr = entry.getKey();
            Move load = entry.getValue();
            StackFrameOperand sfOp = (StackFrameOperand) load.getSource();
            sfOp.setOffset(slotOf.get(vr));
        }
    }

}

