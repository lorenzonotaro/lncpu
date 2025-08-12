package com.lnc.cc.codegen;


import com.lnc.LNC;
import com.lnc.cc.ir.IRBlock;
import com.lnc.cc.ir.IRInstruction;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.ir.Move;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.StackFrameOperand;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.mxgraph.view.mxGraph;

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
            showIg();
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
                it.remove();
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

    private boolean dominated(InterferenceGraph.Node a, InterferenceGraph.Node b) {
        // a is dominated by b if a's palette is subset of b's and a's neighbors ⊆ b's (ignoring each other)
        if (!b.allowedColors().containsAll(a.allowedColors())) return false;
        Set<InterferenceGraph.Node> an = new HashSet<>(a.adj); an.remove(b);
        Set<InterferenceGraph.Node> bn = new HashSet<>(b.adj); bn.remove(a);
        return bn.containsAll(an);
    }

    private boolean ok(InterferenceGraph.Node x, InterferenceGraph.Node y) {
        Set<Register> palette = colorIntersection(x, y);
        if (palette.isEmpty()) return false;

        //if (dominated(x,y) || dominated(y,x)) return true;

        boolean xPseudo = x.isPseudoPhysical();
        boolean yPseudo = y.isPseudoPhysical();
        if (xPseudo ^ yPseudo) return georgeSingleColorDistinct(x, y, palette);
        return briggsPaletteAware(x, y, palette);
    }

    private boolean georgeSingleColorDistinct(InterferenceGraph.Node x,
                                              InterferenceGraph.Node y,
                                              Set<Register> palette) {
        if (palette.size() != 1) return false;

        // v = virtual, p = precolored single-color node
        InterferenceGraph.Node v = x.isPseudoPhysical() ? y : x;
        InterferenceGraph.Node p = x.isPseudoPhysical() ? x : y;
        Register c = palette.iterator().next();

        for (InterferenceGraph.Node t0 : v.adj) {
            InterferenceGraph.Node t = getAlias(t0);
            if (t == v || t == p) continue;

            // t not affected if it can’t use c
            if (!t.allowedColors().contains(c)) continue;

            // If t already adjacent to p, c is already unusable → unaffected
            if (t.adj.contains(p)) continue;

            // After merge, t loses c from its palette
            Set<Register> A = t.allowedColors();
            if (!A.contains(c)) continue;
            Set<Register> Aafter = new HashSet<>(A);
            Aafter.remove(c);

            // Build distinct fixed colors contributed by precolored neighbors of t
            Set<Register> fixedColors = new HashSet<>();
            int riskyNeighbors = 0;

            for (InterferenceGraph.Node u0 : t.adj) {
                InterferenceGraph.Node u = getAlias(u0);
                if (u == t || u == v || u == p) continue;

                // Consider only neighbors that can actually compete for Aafter
                Set<Register> uA = u.allowedColors();
                if (Collections.disjoint(uA, Aafter)) continue;

                if (u.isPseudoPhysical()) {
                    // Precolored: contributes exactly its (single) color if in Aafter
                    if (uA.size() == 1) {
                        Register uc = uA.iterator().next();
                        if (Aafter.contains(uc)) fixedColors.add(uc);
                    } else {
                        // If you ever model multi-color "precolored" (unlikely), treat as risky
                        riskyNeighbors++;
                    }
                } else {
                    // Uncolored neighbor: only matters if it’s "high" for its own palette
                    if (u.degree() >= u.allowedColors().size()) riskyNeighbors++;
                }
            }

            int budget = Aafter.size();
            int consumption = fixedColors.size() + riskyNeighbors;

            // Need strictly less than: at least one color remains for t
            if (consumption >= budget) {
                return false;
            }
        }
        return true;
    }


    private boolean georgePaletteAware(InterferenceGraph.Node x,
                                       InterferenceGraph.Node y,
                                       Set<Register> palette) {
        if (palette.size() != 1) return false;

        InterferenceGraph.Node v = x.isPseudoPhysical() ? y : x; // virtual
        InterferenceGraph.Node p = x.isPseudoPhysical() ? x : y; // single-color
        Register c = palette.iterator().next();

        for (InterferenceGraph.Node t0 : v.adj) {
            InterferenceGraph.Node t = getAlias(t0);
            if (t == v || t == p) continue;

            // t doesn't care about c → unaffected
            if (!t.allowedColors().contains(c)) continue;

            // t already adjacent to p → t already can't take c → unaffected
            if (t.adj.contains(p)) continue;

            // Compute an *effective* degree of t: neighbors that can actually
            // constrain a color from t's palette after we remove c from t.
            int A  = t.allowedColors().size();
            int Aafter = A - 1; // we'll lose c
            int degEff = 0;
            for (InterferenceGraph.Node u0 : t.adj) {
                InterferenceGraph.Node u = getAlias(u0);
                if (u == t || u == v || u == p) continue;

                // If u shares no color with t's palette (sans c), it can't constrain t.
                if (Collections.disjoint(u.allowedColors(), t.allowedColors())) continue;

                // If u is a single-color node equal to c and already adjacent to p,
                // it won't block t from using any non-c color after the merge.
                if (u.isPseudoPhysical() && u.allowedColors().size() == 1) {
                    Register uc = u.allowedColors().iterator().next();
                    if (uc.equals(c) && u.adj.contains(p)) continue;
                }

                degEff++;
            }

            // If after losing c, t still has headroom vs. *effective* constraints, it's fine.
            if (degEff < Aafter) continue;

            // Risky neighbor: reject this coalesce.
            return false;
        }
        return true;
    }



    private boolean briggsPaletteAware(InterferenceGraph.Node x, InterferenceGraph.Node y, Set<Register> palette) {
        int P = palette.size();
        Set<InterferenceGraph.Node> union = new HashSet<>(x.adj);
        union.addAll(y.adj);

        long high = union.stream()
                .map(this::getAlias)
                // neighbors that cannot use any color from the merged palette don't constrain us
                .filter(t -> !Collections.disjoint(t.allowedColors(), palette))
                // ignore trivially-low neighbors: they will simplify anyway
                .filter(t -> t.degree() >= t.allowedColors().size())
                // count only those that are also high relative to *our* future palette size
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
            showIg();
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
                    forbidden.addAll(List.of(w.assigned.getComponents()));
                }
            }

            var okColors = n.allowedColors().stream().filter(c -> !forbidden.contains(c)).collect(Collectors.toCollection(LinkedHashSet::new));

            if (!okColors.isEmpty()) {
                n.assigned = chooseColor(n, okColors, forbidden);
                coloredNodes.add(n);
            } else {
                // real spill
                spillCandidates.add(n);
                return;
            }
            showIg();
        }
    }

    private Register chooseColor(InterferenceGraph.Node n,
                                 Set<Register> okColors,   // allowed(n) minus used neighbor colors
                                 Set<Register> usedColors) // phys colors already used by colored neighbors
    {
        // 0) Strong bias: if we have a single-color move partner (RA/RB/RC/RD) and that color is open, take it.
        for (InterferenceGraph.Node partner0 : n.movePartners) {
            InterferenceGraph.Node partner = getAlias(partner0);
            if (!partner.isPseudoPhysical()) continue;

            Register c = partner.onlyColor();  // your helper for single-color nodes
            if (okColors.contains(c) && !usedColors.contains(c)) {
                return c;  // pick the precolored partner's color, delete the copy later
            }
        }

        // 1) Otherwise, your existing weighted frequency heuristic (but weight by move weights if you have them).
        Map<Register,Integer> freq = new HashMap<>();
        for (InterferenceGraph.Node partner0 : n.movePartners) {
            InterferenceGraph.Node partner = getAlias(partner0);
            if (!coloredNodes.contains(partner)) continue;
            for (Register c : partner.assigned.getComponents()) {
                if (okColors.contains(c) && !usedColors.contains(c)) {
                    int w = moveWeight(n, partner); // 3 for entry demotes, 2 for call args, 1 default
                    freq.merge(c, w, Integer::sum);
                }
            }
        }

        if (!freq.isEmpty()) {
            return freq.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();
        }

        // 2) Fallback: pick any available color deterministically.
        return okColors.iterator().next();
    }

    private int moveWeight(InterferenceGraph.Node n, InterferenceGraph.Node partner) {
        return 1; // TODO: implement move weights
    }


    int spillHarm(Register c) {
        return coloredNodes.stream()
                .filter(v -> Set.of(v.assigned.getComponents()).contains(c))
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
            InterferenceGraphVisualizer.setGraph(ig.getVirtualNodes());
            showIg();

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

    private static void showIg() {
        if(LNC.settings.get("--print-ig", Boolean.class)) {
            InterferenceGraphVisualizer.showVisualizer();
        }
    }

    private static void updateGraph(mxGraph mxGraph, Collection<InterferenceGraph.Node> virtualNodes) {
        mxGraph.getModel().beginUpdate();
        mxGraph.removeCells();
        try{
            for(InterferenceGraph.Node n : virtualNodes) {
                mxGraph.insertVertex(mxGraph.getDefaultParent(), null, n.vr.getRegisterNumber(), 0, 0, 80, 30);
            }
        }finally {
            mxGraph.getModel().endUpdate();
        }
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

