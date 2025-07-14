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

    private final Map<VirtualRegister, Integer> spillCost = new LinkedHashMap<>();

    // aliasing and degree tracking
    private final Map<InterferenceGraph.Node,InterferenceGraph.Node> alias = new HashMap<>();
    private final Set<AbstractMap.SimpleEntry<InterferenceGraph.Node,InterferenceGraph.Node>> worklistMoves = new LinkedHashSet<>();

    public GraphColoringRegisterAllocator(InterferenceGraph graph) {
        this.graph = graph;

        computeSpillCosts();

        // count physical nodes (excluding compounds if you treat them specially)
        this.K = graph.getPhysicalNodes().size();

        coloredNodes.addAll(graph.getPhysicalNodes());

        worklistMoves.addAll(graph.getMoveEdges());

        this.doCoalesce = !LNC.settings.get("--reg-alloc-no-coalesce", Boolean.class);
    }

    private void computeSpillCosts() {
        for(var node : graph.getVirtualNodes()){
            int loopWeight = graph.getLoopWeights().getOrDefault(node.vr, 1);
            int base = graph.getUses().getOrDefault(node.vr, 0) + 2 * graph.getDefs().getOrDefault(node.vr, 0);
            int span = graph.getLiveRanges().get(node.vr).getSpan() / 10;
            int size = node.vr.getTypeSpecifier().allocSize();
            int classWt = K / node.allowedColors().size();
            int cost = loopWeight * (base + span) * size * classWt;
            spillCost.put(node.vr, cost);
        }
    }

    public void allocate() {
        if(doCoalesce) {
            coalesce();
        }
        simplify();
        select();
        assignColors();
    }

    int spillCost(InterferenceGraph.Node n) {          // use your existing metric
        return n.vr.spillCost();
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
        /* 1.  Reject if classes are incompatible */
        Set<Register> palette = colorIntersection(x, y);
        if (palette.isEmpty()) return false;

        int P = palette.size();

        if (x.isPseudoPhysical() && !y.isPseudoPhysical())
            return george(y, x, P);        // y virtual, x pseudo-phys
        if (y.isPseudoPhysical() && !x.isPseudoPhysical())
            return george(x, y, P);        // x virtual, y pseudo-phys

        /* otherwise fall back to Briggs with palette-aware threshold */
        return briggs(x, y, P);
    }

    private boolean george(InterferenceGraph.Node a, InterferenceGraph.Node b, int P) {
        InterferenceGraph.Node v = a.isPhysical() ? b : a;      // the virtual endpoint
        InterferenceGraph.Node p = a.isPhysical() ? a : b;      // the physical endpoint
        for (InterferenceGraph.Node t : v.adj) {
            t = getAlias(t);
            if (t == p) continue;
            if (t.degree() >= P && !t.adj.contains(p))
                return false;
        }
        return true;
    }

    private static boolean briggs(InterferenceGraph.Node x, InterferenceGraph.Node y, int P) {
        Set<InterferenceGraph.Node> union = new HashSet<>(x.adj);
        union.addAll(y.adj);
        long high = union.stream()
                .filter(t -> t.degree() >= P)
                .count();
        return high < P;
    }

    private Set<Register> colorIntersection(InterferenceGraph.Node x, InterferenceGraph.Node y) {
        var set = new LinkedHashSet<>(x.allowedColors());
        set.retainAll(y.allowedColors());
        return set;
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
        x.vr.setRegisterClass(RegisterClass.of(colorIntersection(x, y)));
        x.movePartners.addAll(y.movePartners);
    }

    private void simplify() {
        var work = new LinkedHashMap<InterferenceGraph.Node, Set<InterferenceGraph.Node>>();
        for (var n : graph.getVirtualNodes()) {
            n = getAlias(n); // ensure we have the alias
            if (work.containsKey(n))
                continue;
            Set<InterferenceGraph.Node> nbrs =
                    n.adj.stream()
                            .map(this::getAlias)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            work.put(n, new LinkedHashSet<>(nbrs));
        }

        // removal loop unchanged
        while (!work.isEmpty()) {
            var maybe = work.keySet().stream()
                    .filter(n -> work.get(n).size() < n.allowedColors().size())
                    .max(Comparator.comparingInt(n -> n.allowedColors().size()));
            InterferenceGraph.Node n;

            n = maybe.orElseGet(() -> work.keySet().stream()
                    .min(Comparator
                            .comparingInt((InterferenceGraph.Node m) -> spillCost.get(m.vr)) // ★
                            .thenComparingInt(m -> m.vr.getRegisterNumber()))
                    .get());

            for (var neighbor : work.get(n)) {
                Set<InterferenceGraph.Node> nbrs = work.get(neighbor);
                if (nbrs != null) {               // neighbour still in the graph
                    nbrs.remove(n);               // decrement its degree
                }
            }
            work.remove(n);                       // finally delete n itself
            selectStack.push(n);
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

            Map<Register,Integer> freq = new HashMap<>();          // colour → count
            for (InterferenceGraph.Node p : n.movePartners) {
                p = getAlias(p);
                if (coloredNodes.contains(p)) {
                    Register c = p.assigned;
                    if (n.allowedColors().contains(c) && !forbidden.contains(c))
                        freq.merge(c, 1, Integer::sum);
                }
            }

            // pick an allowed color not forbidden
            Optional<Register> maybeColor = n.allowedColors().stream()
                    .filter(c -> !forbidden.contains(c))
                    .min(Comparator
                            .comparingInt((Register c) -> -freq.getOrDefault(c, 0))            // larger freq wins
                            .thenComparingInt(this::spillHarm)                   // lower harm wins
                            .thenComparingInt(Register::ordinal));

            if (maybeColor.isPresent()) {
                n.assigned = maybeColor.get();
                coloredNodes.add(n);
            } else {
                // real spill
                spillCandidates.add(n);
                return;
            }
        }
    }

    int spillHarm(Register c) {
        return coloredNodes.stream()
                .filter(v -> v.assigned == c)
                .mapToInt(v -> spillCost.getOrDefault(v.vr, 0))
                .sum();
    }

    private void assignColors() {
        // precolored nodes already have assigned = precolored.get()
        // coloredNodes have their .assigned set
        // spillCandidates need spill code insertion downstream

        for(var n : coloredNodes) {
            // Skip any that were also pre-colored

            if (n.isPhysical()) continue;

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


    public static AllocationInfo run(IRUnit unit){

        int maxIter = LNC.settings.get("--reg-alloc-max-iter", Double.class).intValue();

        Set<InterferenceGraph.Node> spills;
        List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillStores = new ArrayList<>();
        List<AbstractMap.SimpleEntry<VirtualRegister, Move>>  spillLoads  = new ArrayList<>();

        SpillSlotAssigner slotAssigner = new SpillSlotAssigner();

        Set<Register> usedRegisters = new LinkedHashSet<>();

        InterferenceGraph ig = null;
        LivenessInfo livenessInfo = null;

        do{

            if(maxIter-- <= 0) {
                throw new RuntimeException("Exceeded maximum iterations for register allocation.");
            }

            // 1) Build graph & run allocator
            ig = InterferenceGraph.buildInterferenceGraph(unit);
            if(LNC.settings.get("--print-ig", Boolean.class)) {
                System.out.println("Interference Graph:\n" + ig);
            }

            GraphColoringRegisterAllocator allocator = new GraphColoringRegisterAllocator(ig);
            allocator.allocate();
            spills = allocator.getSpills();

            usedRegisters = allocator.getUsedRegisters();

            if(!spills.isEmpty()) {
                // 2) Insert spill code for each spilled vreg
                for (IRBlock bb : unit.computeReversePostOrderAndCFG()) {
                    for (IRInstruction inst = bb.getFirst(); inst != null; inst = inst.getNext()) {
                        // after defs
                        for (VirtualRegister vr : inst.getWrites()) {
                            if (spills.contains(ig.getNode(vr))) {
                                Move move = new Move(vr, new StackFrameOperand(vr.getTypeSpecifier(), StackFrameOperand.OperandType.LOCAL, 0));
                                spillStores.add(new AbstractMap.SimpleEntry<>(vr, move));
                                inst.insertAfter(move);
                            }
                        }
                        // before uses
                        for (VirtualRegister vr : inst.getReads()) {
                            if (spills.contains(ig.getNode(vr))) {
                                // allocate a temp for the loaded value
                                VirtualRegister temp = unit.getVirtualRegisterManager().getRegister(vr.getTypeSpecifier());
                                temp.setRegisterClass(vr.getRegisterClass());
                                Move load = new Move(new StackFrameOperand(vr.getTypeSpecifier(), StackFrameOperand.OperandType.LOCAL, 0), temp);
                                spillLoads.add(new AbstractMap.SimpleEntry<>(vr, load));
                                inst.insertBefore(load);
                                inst.replaceOperand(vr, temp);
                            }
                        }
                    }
                }

                Map<VirtualRegister, LiveRange> allRanges = ig.getLiveRanges();
                InterferenceGraph finalIg = ig;
                Set<InterferenceGraph.Node> finalSpills = spills;
                Map<VirtualRegister, LiveRange> spillRanges = allRanges.entrySet().stream()
                        .filter(e -> finalSpills.contains(finalIg.getNode(e.getKey())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                slotAssigner.assignSlots(spillRanges);
                patchSpillOffsets(spillStores, spillLoads, slotAssigner.slotOffset);
            }

            livenessInfo = LivenessInfo.computeBlockLiveness(unit);

            // loop back and re-allocate on the new IR (temps + spill code)
        }while(!spills.isEmpty());

        unit.setSpillSpaceSize(slotAssigner.getTotalSlots());
        unit.setUsedRegisters(usedRegisters);

        return new AllocationInfo(ig, livenessInfo);
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

    public record AllocationInfo(InterferenceGraph interferenceGraph, LivenessInfo livenessInfo) {
    }
}

