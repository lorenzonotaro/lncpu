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
    private final boolean doCoalesce;

    // worklists and stacks
    private Deque<InterferenceGraph.Node> selectStack = new ArrayDeque<>();
    private final Set<InterferenceGraph.Node> spillCandidates = new LinkedHashSet<>();
    private final Set<InterferenceGraph.Node> coloredNodes    = new LinkedHashSet<>();
    private final Set<Register> usedRegisters = new LinkedHashSet<>();

    // aliasing and degree tracking
    private final Map<InterferenceGraph.Node,InterferenceGraph.Node> alias = new HashMap<>();
    private final Map<InterferenceGraph.Node,Integer>                degree = new HashMap<>();
    private final Set<AbstractMap.SimpleEntry<InterferenceGraph.Node,InterferenceGraph.Node>> worklistMoves = new LinkedHashSet<>();

    public GraphColoringRegisterAllocator(InterferenceGraph graph) {
        this.graph = graph;

        // count physical nodes (excluding compounds if you treat them specially)
        this.K = graph.getPhysicalNodes().size();

        worklistMoves.addAll(graph.getMoveEdges());

        this.doCoalesce = !LNC.settings.get("--reg-alloc-no-coalesce", Boolean.class);
    }

    public void allocate() {
        if(doCoalesce) {
            coalesce();
        }
        simplify();
        select();
        assignColors();
    }

    // ----------------------------------------------------------------------
    // 1. Copy‑coalescing ----------------------------------------------------
    private void coalesce() {
        Iterator<AbstractMap.SimpleEntry<InterferenceGraph.Node,InterferenceGraph.Node>> it = worklistMoves.iterator();
        while (it.hasNext()) {
            var mv = it.next();
            InterferenceGraph.Node x = getAlias(mv.getKey());
            InterferenceGraph.Node y = getAlias(mv.getValue());
            if (x == y) { it.remove(); continue; }

            if (!x.adj.contains(y) && ok(x, y)) {
                combine(x, y);
            }
        }
    }


    private InterferenceGraph.Node getAlias(InterferenceGraph.Node n) {
        InterferenceGraph.Node a = alias.get(n);
        if (a == null) return n;
        a = getAlias(a);          // path compression
        alias.put(n, a);
        return a;
    }

    private boolean ok(InterferenceGraph.Node x, InterferenceGraph.Node y) {
        for (InterferenceGraph.Node t : x.adj) {
            t = getAlias(t);
            if (t == y) continue;
            if (!t.adj.contains(y) && degree.getOrDefault(t, t.adj.size()) >= K)
                return false;
        }
        return true;
    }

    private void combine(InterferenceGraph.Node x, InterferenceGraph.Node y) {
        alias.put(y, x);               // y → x
        // move adjacency of y into x
        for (InterferenceGraph.Node n : new ArrayList<>(y.adj)) {
            y.adj.remove(n);
            n.adj.remove(y);
            if (n != x) {
                x.adj.add(n); n.adj.add(x);
            }
        }
        // update degree
        degree.put(x, x.adj.size());
    }

    private void simplify() {
        // build mutable work graph only over non-precolored nodes
        var work = new ArrayList<>(graph.getVirtualNodes().stream()
                .sorted(
                        Comparator
                                .comparingInt((InterferenceGraph.Node n) -> n.allowedColors().size())
                                .thenComparingInt(n -> n.adj.size())
                )
                .toList());
        Collections.reverse(work);
        for (var n : work) {
            // precolor nodes that have allowedColors.size() == 1
            if (n.allowedColors().size() == 1) {
                n.assigned = n.allowedColors().iterator().next();
                coloredNodes.add(n);
            }else{
                // add to selectStack
                selectStack.push(n);
            }
        }
    }
    private void select() {
        while (!selectStack.isEmpty()) {
            var n = selectStack.pop();

            // compute forbidden colors from already‐colored neighbors
            Set<Register> forbidden = new HashSet<>();
            for (var w : n.adj) {
                w = getAlias(w);
                if (coloredNodes.contains(w)) {
                    forbidden.add(w.assigned);
                }
            }

            // pick an allowed color not forbidden
            Optional<Register> maybeColor = n.allowedColors().stream()
                    .filter(c -> !forbidden.contains(c))
                    .min(Comparator.comparingInt(Register::ordinal));

            if (maybeColor.isPresent()) {
                n.assigned = maybeColor.get();
                coloredNodes.add(n);
            } else {
                // real spill
                spillCandidates.add(n);
            }
        }
    }

    private void assignColors() {
        // precolored nodes already have assigned = precolored.get()
        // coloredNodes have their .assigned set
        // spillCandidates need spill code insertion downstream

        for(var n : coloredNodes) {
            // Skip any that were also pre-colored
            n.vr.setAssignedPhysicalRegister(n.assigned);
            this.usedRegisters.add(n.assigned);
        }

        // ✨ propagate colours to aliases
        for (InterferenceGraph.Node n : graph.getVirtualNodes()) {
            InterferenceGraph.Node rep = getAlias(n);
            if (n.assigned == null && rep.assigned != null) {
                n.assigned = rep.assigned;
                n.vr.setAssignedPhysicalRegister(rep.assigned);
                usedRegisters.add(rep.assigned);
            }
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
            if(LNC.settings.get("--print-ig", Boolean.class)) {
                System.out.println("Interference Graph:\n" + ig);
            }

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

