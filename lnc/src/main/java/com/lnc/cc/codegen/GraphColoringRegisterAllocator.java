package com.lnc.cc.codegen;


import com.lnc.LNC;
import com.lnc.cc.ir.IRBlock;
import com.lnc.cc.ir.IRInstruction;
import com.lnc.cc.ir.IRUnit;
import com.lnc.cc.ir.Pop;
import com.lnc.cc.ir.Move;
import com.lnc.cc.ir.Push;
import com.lnc.cc.ir.operands.ImmediateOperand;
import com.lnc.cc.ir.operands.StackFrameLocation;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.optimization.ir.StageOneIROptimizer;
import com.mxgraph.view.mxGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The GraphColoringRegisterAllocator class implements a register allocation algorithm
 * using graph coloring. This class is responsible for assigning physical machine
 * registers to variables in a program while optimizing code performance and minimizing
 * the need for register spilling.
 *
 * The allocator operates on an interference graph where nodes represent program variables
 * or temporaries, and edges represent conflicts between them, indicating that two variables
 * cannot share the same register.
 *
 * Core functionalities:
 * - Building the interference graph and identifying conflicts.
 * - Applying the graph coloring technique to allocate registers.
 * - Performing optimizations such as copy coalescing to minimize unnecessary register moves.
 * - Handling register spilling by inserting loads and stores when there are insufficient registers.
 */
public class GraphColoringRegisterAllocator {
    private final InterferenceGraph graph;
    private final int K;  // total number of physical “colors”
    private final boolean doCoalesce;

    // worklists and stacks
    private Deque<InterferenceGraph.Node> selectStack = new ArrayDeque<>();
    private final List<InterferenceGraph.Node> spillCandidates = new ArrayList<>();
    private final Set<InterferenceGraph.Node> coloredNodes    = new LinkedHashSet<>();
    private final Set<Register> usedRegisters = new LinkedHashSet<>();

    private final Map<VirtualRegister, Integer> spillCost = new LinkedHashMap<>();

    // simplify work graph (rebuilt when the interference graph changes via coalescing)
    private LinkedHashMap<InterferenceGraph.Node, Set<InterferenceGraph.Node>> simplifyWork = new LinkedHashMap<>();

    // aliasing and degree tracking
    private final Map<InterferenceGraph.Node,InterferenceGraph.Node> alias = new HashMap<>();
    private final Set<AbstractMap.SimpleEntry<InterferenceGraph.Node,InterferenceGraph.Node>> worklistMoves = new LinkedHashSet<>();

    public GraphColoringRegisterAllocator(InterferenceGraph graph) {
        this.graph = graph;
        // count physical nodes (excluding compounds if you treat them specially)
        this.K = graph.getPhysicalNodes().size();

        computeSpillCosts();

        coloredNodes.addAll(graph.getPhysicalNodes());

        worklistMoves.addAll(graph.getMoveEdges());

        this.doCoalesce = !LNC.settings.get("--reg-alloc-no-coalesce", Boolean.class);
    }

    private void computeSpillCosts() {
        spillCost.clear();
        for(var node : graph.getVirtualNodes()){
            int loopWeight = graph.getLoopWeights().getOrDefault(node.vr, 1);
            int base = graph.getUses().getOrDefault(node.vr, 0) + 2 * graph.getDefs().getOrDefault(node.vr, 0);
            LiveRange range = graph.getLiveRanges().get(node.vr);
            int span = range == null ? 0 : Math.max(0, range.getSpan() / 10);
            int size = node.vr.getTypeSpecifier().allocSize();
            int classSize = Math.max(1, node.allowedColors().size());
            int classWt = Math.max(1, K / classSize);
            int hotness = Math.max(1, base + span);

            // Prefer spilling low-utility nodes, but discount nodes that are already
            // highly constrained: they are more likely to be the source of color pressure.
            int degreePressure = Math.max(1, node.degree() - classSize + 1);

            // Copy-related nodes are more expensive to spill because spilling them can
            // destroy coalescing opportunities and introduce extra reloads.
            int movePenalty = Math.max(1, node.movePartners.size() + 1);

            long cost = (long) loopWeight + hotness * 5 + size * 2 + classWt * 2 + movePenalty;
            cost = Math.max(1L, cost / degreePressure);
            spillCost.put(node.vr, (int) Math.min(Integer.MAX_VALUE, cost));
        }
    }

    public void allocate() {
        spillCandidates.clear();
        // Initialize the simplify work graph
        buildSimplifyWork();
        computeSpillCosts();

        boolean progress;
        do {
            progress = false;

            // Try to coalesce as much as possible; rebuild simplify graph when the topology changes
            if (doCoalesce) {
                boolean merged;
                do {
                    merged = coalesce();
                    if (merged) {
                        progress = true;
                        buildSimplifyWork();
                        computeSpillCosts();
                    }
                } while (merged);
            }

            // Remove a single node (either trivially colorable or chosen as a spill candidate)
            boolean didSimplify = simplifyStep();
            if (didSimplify) {
                progress = true;
            }

        } while (progress);

        // Color in reverse removal order
        select();
        assignColors();
    }


    // ----------------------------------------------------------------------
    // 1. Copy‑coalescing ----------------------------------------------------
    private boolean coalesce() {
        boolean changed = false;
        Iterator<AbstractMap.SimpleEntry<InterferenceGraph.Node,InterferenceGraph.Node>> it = worklistMoves.iterator();
        while (it.hasNext()) {
            var mv = it.next();
            InterferenceGraph.Node x = getAlias(mv.getKey());
            InterferenceGraph.Node y = getAlias(mv.getValue());
            if (x == y) { it.remove(); continue; }

            if (!x.adj.contains(y) && ok(x, y)) {
                combine(x, y);
                it.remove();
                changed = true;
            }
        }
        return changed;
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

    private void buildSimplifyWork() {
        simplifyWork.clear();
        for (var n0 : graph.getVirtualNodes()) {
            var n = getAlias(n0); // ensure we consider the current representative
            if (simplifyWork.containsKey(n))
                continue;
            Set<InterferenceGraph.Node> nbrs =
                    n.adj.stream()
                            .map(this::getAlias)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            // remove potential self-edges created by aliasing
            nbrs.remove(n);
            simplifyWork.put(n, new LinkedHashSet<>(nbrs));
        }
    }

    private boolean simplifyStep() {
        if (simplifyWork.isEmpty()) return false;

        // Prefer a trivially-colorable node; otherwise choose a spill candidate
        Optional<InterferenceGraph.Node> maybe =
                simplifyWork.keySet().stream()
                        .filter(n -> simplifyWork.get(n).size() < n.allowedColors().size())
                        .max(Comparator.comparingInt(n -> n.allowedColors().size()));

        InterferenceGraph.Node n = maybe.orElseGet(() -> simplifyWork.keySet().stream()
                .min(Comparator
                        .comparingInt((InterferenceGraph.Node m) -> spillCost.get(m.vr))
                        .thenComparingInt(m -> m.vr.getRegisterNumber()))
                .orElseThrow());

        for (var neighbor : simplifyWork.get(n)) {
            Set<InterferenceGraph.Node> nbrs = simplifyWork.get(neighbor);
            if (nbrs != null) {               // neighbour still in the graph
                nbrs.remove(n);               // decrement its degree
            }
        }
        simplifyWork.remove(n);               // finally delete n itself
        selectStack.push(n);
        return true;
    }
    private void select() {
        while (!selectStack.isEmpty()) {
            var n = selectStack.pop();

            // Collect assigned colors from already-colored neighbors.
            Set<Register> usedNeighborColors = new LinkedHashSet<>();
            for (var w : n.adj) {
                w = getAlias(w);
                if (coloredNodes.contains(w) && w.assigned != null) {
                    usedNeighborColors.add(w.assigned);
                }
            }
            var okColors = n.allowedColors().stream()
                    .filter(c -> usedNeighborColors.stream().noneMatch(used -> overlaps(c, used)))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (!okColors.isEmpty()) {
                n.assigned = chooseColor(n, okColors, usedNeighborColors);
                coloredNodes.add(n);
            } else {
                // Spill one of the colored neighbors that is actually blocking this node.
                InterferenceGraph.Node spillCandidate = chooseSpillVictim(n, usedNeighborColors);
                spillCandidates.add(spillCandidate == null ? n : spillCandidate);
                return;
            }
        }
    }

    private InterferenceGraph.Node chooseSpillVictim(InterferenceGraph.Node failedNode,
                                                     Set<Register> usedNeighborColors) {
        Set<InterferenceGraph.Node> blockers = failedNode.adj.stream()
                .map(this::getAlias)
                .filter(neighbor -> neighbor != failedNode)
                .filter(neighbor -> !neighbor.isPhysical())
                .filter(coloredNodes::contains)
                .filter(neighbor -> neighbor.assigned != null)
                .filter(neighbor -> usedNeighborColors.stream().anyMatch(used -> overlaps(used, neighbor.assigned)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (blockers.isEmpty()) {
            return failedNode.isPhysical() ? null : failedNode;
        }

        return blockers.stream()
                .min(Comparator
                        .comparingInt((InterferenceGraph.Node m) -> spillCost.getOrDefault(m.vr, Integer.MAX_VALUE))
                        .thenComparingInt(InterferenceGraph.Node::degree)
                        .thenComparingInt(m -> m.vr.getRegisterNumber()))
                .orElse(failedNode);
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
            if (okColors.contains(partner.assigned) && !usedColors.contains(partner.assigned)) {
                int w = moveWeight(n, partner); // 3 for entry demotes, 2 for call args, 1 default
                freq.merge(partner.assigned, w, Integer::sum);
            }
        }

        if (!freq.isEmpty()) {
            return freq.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElseThrow()
                    .getKey();
        }

        // 2) Fallback: pick any available color deterministically.
        return okColors.iterator().next();
    }

    private static boolean overlaps(Register a, Register b) {
        for (Register ac : a.getComponents()) {
            for (Register bc : b.getComponents()) {
                if (ac == bc) {
                    return true;
                }
            }
        }
        return false;
    }

    private int moveWeight(InterferenceGraph.Node n, InterferenceGraph.Node partner) {
        return 1; // TODO: implement move weights
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

        verifyColoringSafety();

        // at this point, any node not in coloredNodes ∪ precoloredNodes is spilled
    }

    private Set<Register> getUsedRegisters() {
        return usedRegisters;
    }

    private void verifyColoringSafety() {
        for (InterferenceGraph.Node n0 : graph.getVirtualNodes()) {
            InterferenceGraph.Node n = getAlias(n0);
            Register nc = n.assigned;
            if (nc == null) continue;

            if (!n.allowedColors().contains(nc)) {
                throw new IllegalStateException("Assigned color outside register class for " + n + ": " + nc);
            }

            for (InterferenceGraph.Node m0 : n.adj) {
                InterferenceGraph.Node m = getAlias(m0);
                Register mc = m.assigned;
                if (mc == null) continue;
                if (overlaps(nc, mc)) {
                    throw new IllegalStateException("Interference violation between " + n + "=" + nc + " and " + m + "=" + mc);
                }
            }
        }
    }

    private Set<VirtualRegister> getSpilledVirtualRegisters(InterferenceGraph.Node spillNode) {
        if (spillNode == null) return Collections.emptySet();
        InterferenceGraph.Node rep = getAlias(spillNode);
        Set<VirtualRegister> spilled = new LinkedHashSet<>();
        for (InterferenceGraph.Node n : graph.getVirtualNodes()) {
            if (getAlias(n) == rep) {
                spilled.add(n.vr);
            }
        }
        return spilled;
    }

    public InterferenceGraph.Node getSpillCandidate() {
        return spillCandidates.stream().filter(Objects::nonNull).min(
                Comparator.comparingInt((InterferenceGraph.Node n) -> spillCost.get(n.vr))
                .thenComparingInt(n -> n.vr.getRegisterNumber())).orElse(null);
    }


    public static AllocationInfo run(IRUnit unit){

        int maxIter = LNC.settings.get("--reg-alloc-max-iter", Double.class).intValue();

        InterferenceGraph.Node spillCandidate;
        List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillStores = new ArrayList<>();
        List<AbstractMap.SimpleEntry<VirtualRegister, Move>>  spillLoads  = new ArrayList<>();

        SpillSlotAssigner slotAssigner = new SpillSlotAssigner();

        Set<Register> usedRegisters = new LinkedHashSet<>();

        InterferenceGraph ig = null;
        LivenessInfo livenessInfo = LivenessInfo.computeBlockLiveness(unit);

        // Safety reset: stale assignments from a previous allocation pass should never leak
        // into a fresh run.
        unit.getVirtualRegisterManager().clearAssignedPhysicalRegisters();

        do{

            if(maxIter-- <= 0) {
                throw new RuntimeException("Exceeded maximum iterations for register allocation.");
            }

            // 1) Build graph & run allocator
            unit.getVirtualRegisterManager().clearAssignedPhysicalRegisters();
            ig = InterferenceGraph.buildInterferenceGraph(unit);
            InterferenceGraphVisualizer.setGraph(ig.getVirtualNodes());

            GraphColoringRegisterAllocator allocator = new GraphColoringRegisterAllocator(ig);
            allocator.allocate();
            spillCandidate = allocator.getSpillCandidate();

            usedRegisters = allocator.getUsedRegisters();

            if(spillCandidate != null) {

                spillStores.clear();
                spillLoads.clear();
                Set<VirtualRegister> spilledVirtuals = allocator.getSpilledVirtualRegisters(spillCandidate);

                // 2) Insert spill code using conservative same-block live-range splitting.
                // Load once at the beginning of a merged segment, keep the temporary live
                // across adjacent touches when liveness and palette pressure allow it, and
                // store once at the end of the segment.
                for (IRBlock bb : unit.computeReversePostOrderAndCFG()) {
                    for (IRInstruction inst = bb.getFirst(); inst != null; inst = inst.getNext()) {
                        if (!(inst instanceof Move mv && mv.isRegParamDemotion())) {
                            continue;
                        }

                        for (VirtualRegister vr : spilledVirtuals) {
                            if (!inst.getReads().contains(vr) && !inst.getWrites().contains(vr)) {
                                continue;
                            }

                            inst.replaceOperand(vr, new StackFrameLocation(vr.getTypeSpecifier(), StackFrameLocation.OperandType.LOCAL, 0));
                            spillStores.add(new AbstractMap.SimpleEntry<>(vr, mv));
                        }
                    }
                }

                Map<VirtualRegister, List<SpillSegment>> spillSegments = planSpillSegments(unit, livenessInfo, ig, spilledVirtuals);
                for (VirtualRegister vr : spilledVirtuals) {
                    for (SpillSegment segment : spillSegments.getOrDefault(vr, Collections.emptyList())) {
                        applySpillSegment(unit, vr, segment, livenessInfo, spillStores, spillLoads);
                    }
                }

                Map<VirtualRegister, LiveRange> allRanges = ig.getLiveRanges();
                Map<VirtualRegister, LiveRange> spillRanges = allRanges.entrySet().stream()
                        .filter(e -> spilledVirtuals.contains(e.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                slotAssigner.assignSlots(spillRanges);
                patchSpillOffsets(spillStores, spillLoads, slotAssigner.slotOffset);

                maybeRunPostSpillIROptimizer(unit);
            }

            livenessInfo = LivenessInfo.computeBlockLiveness(unit);

            // loop back and re-allocate on the new IR (temps + spill code)
        }while(spillCandidate != null);

        unit.setSpillSpaceSize(slotAssigner.getTotalSlots());
        unit.setUsedRegisters(usedRegisters);

        return new AllocationInfo(ig, livenessInfo);
    }

    private static boolean tryFoldImmediateSpillStore(IRInstruction inst,
                                                      VirtualRegister spilledVr,
                                                      List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillStores) {
        if (!(inst instanceof Move defMove)) {
            return false;
        }
        if (!(defMove.getSource() instanceof ImmediateOperand)) {
            return false;
        }
        if (!(defMove.getDest() instanceof VirtualRegister destVr) || destVr != spilledVr) {
            return false;
        }

        StackFrameLocation slot = new StackFrameLocation(
                spilledVr.getTypeSpecifier(),
                StackFrameLocation.OperandType.LOCAL,
                0
        );
        defMove.setDest(slot);
        spillStores.add(new AbstractMap.SimpleEntry<>(spilledVr, defMove));
        return true;
    }

    private record SpillSegment(IRBlock block,
                                List<IRInstruction> instructions,
                                int startIndex,
                                int endIndex) {
    }

    private static Map<VirtualRegister, List<SpillSegment>> planSpillSegments(IRUnit unit,
                                                                              LivenessInfo livenessInfo,
                                                                              InterferenceGraph graph,
                                                                              Set<VirtualRegister> spilledVirtuals) {
        Map<VirtualRegister, List<SpillSegment>> spillSegments = new LinkedHashMap<>();
        List<IRBlock> blocks = unit.computeReversePostOrderAndCFG();

        for (VirtualRegister vr : spilledVirtuals) {
            InterferenceGraph.Node spillNode = graph.getNode(vr);
            List<SpillSegment> segments = new ArrayList<>();

            for (IRBlock block : blocks) {
                List<IRInstruction> instructions = new ArrayList<>();
                for (IRInstruction inst = block.getFirst(); inst != null; inst = inst.getNext()) {
                    instructions.add(inst);
                }

                List<Integer> touchIndices = directUseIndices(instructions, vr);
                if (touchIndices.isEmpty()) {
                    continue;
                }

                int segmentStart = touchIndices.get(0);
                int segmentEnd = segmentStart;

                for (int i = 1; i < touchIndices.size(); i++) {
                    int nextTouch = touchIndices.get(i);
                    if (canExtendSpillSegment(vr, instructions, livenessInfo, spillNode, segmentStart, nextTouch)) {
                        segmentEnd = nextTouch;
                    } else {
                        segments.add(new SpillSegment(block, instructions, segmentStart, segmentEnd));
                        segmentStart = segmentEnd = nextTouch;
                    }
                }

                segments.add(new SpillSegment(block, instructions, segmentStart, segmentEnd));
            }

            spillSegments.put(vr, segments);
        }

        return spillSegments;
    }

    private static boolean canExtendSpillSegment(VirtualRegister vr,
                                                 List<IRInstruction> instructions,
                                                 LivenessInfo livenessInfo,
                                                 InterferenceGraph.Node spillNode,
                                                 int segmentStartIndex,
                                                 int candidateEndIndex) {
        Set<Register> palette = spillNode.allowedColors();
        int paletteBudget = palette.size();
        IRInstruction candidateTouch = instructions.get(candidateEndIndex);
        boolean candidateWritesVr = candidateTouch.getWrites().contains(vr);

        for (int i = segmentStartIndex; i < candidateEndIndex; i++) {
            IRInstruction inst = instructions.get(i);

            // The lowered three-address form often moves the "variable value" through
            // short-lived temporaries, so the original spilled vreg can be dead between
            // an input read and a later write-back touch. Allow extending such spans only
            // when the next touch writes the spilled vreg.
            if (!livenessInfo.isLiveAfter(vr, inst) && !candidateWritesVr) {
                return false;
            }

            int blockers = 0;
            for (VirtualRegister liveVr : livenessInfo.getLiveAfter(inst)) {
                if (liveVr.equals(vr)) {
                    continue;
                }

                if (Collections.disjoint(liveVr.getRegisterClass().getRegisters(), palette)) {
                    continue;
                }

                blockers++;
                if (blockers >= paletteBudget) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void applySpillSegment(IRUnit unit,
                                          VirtualRegister vr,
                                          SpillSegment segment,
                                          LivenessInfo livenessInfo,
                                          List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillStores,
                                          List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillLoads) {
        IRInstruction start = segment.instructions().get(segment.startIndex());
        IRInstruction end = segment.instructions().get(segment.endIndex());
        boolean hasWrite = segmentHasWrite(vr, segment);

        if (segment.startIndex() == segment.endIndex() && !start.getReads().contains(vr)
                && tryFoldImmediateSpillStore(start, vr, spillStores)) {
            return;
        }

        VirtualRegister temp = unit.getVirtualRegisterManager().getRegister(vr.getTypeSpecifier());
        temp.setRegisterClass(vr.getRegisterClass());

        if (start.getReads().contains(vr)) {
            Move load = new Move(
                    new StackFrameLocation(vr.getTypeSpecifier(), StackFrameLocation.OperandType.LOCAL, 0),
                    temp
            );
            spillLoads.add(new AbstractMap.SimpleEntry<>(vr, load));
            start.insertBefore(load);
        }

        for (int i = segment.startIndex(); i <= segment.endIndex(); i++) {
            IRInstruction inst = segment.instructions().get(i);
            if (inst.getReads().contains(vr) || inst.getWrites().contains(vr)) {
                inst.replaceOperand(vr, temp);
            }
        }

        if (hasWrite && livenessInfo.isLiveAfter(vr, end)) {
            Move store = new Move(
                    temp,
                    new StackFrameLocation(vr.getTypeSpecifier(), StackFrameLocation.OperandType.LOCAL, 0)
            );
            spillStores.add(new AbstractMap.SimpleEntry<>(vr, store));
            end.insertAfter(store);
        }
    }

    private static boolean segmentHasWrite(VirtualRegister vr, SpillSegment segment) {
        for (int i = segment.startIndex(); i <= segment.endIndex(); i++) {
            if (segment.instructions().get(i).getWrites().contains(vr)) {
                return true;
            }
        }
        return false;
    }

    private static boolean maybeRunPostSpillIROptimizer(IRUnit unit) {
        if (!LNC.settings.get("--reg-alloc-post-spill-ir-opt", Boolean.class)) {
            return false;
        }

        return new StageOneIROptimizer().run(unit);
    }

    private static boolean tryConservativeSameBlockSplit(IRUnit unit,
                                                        LivenessInfo livenessInfo,
                                                        InterferenceGraph.Node spillCandidate,
                                                        Map<VirtualRegister, Integer> spillCost) {
        if (spillCandidate == null || spillCandidate.vr == null) {
            return false;
        }

        Set<VirtualRegister> candidateNeighbors = spillCandidate.adj.stream()
                .map(n -> n.vr)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (candidateNeighbors.isEmpty()) {
            return false;
        }

        for (IRBlock block : unit.computeReversePostOrderAndCFG()) {
            if (block.getFirst() == null || block.getLast() == null) {
                continue;
            }

            if (livenessInfo.liveOut().getOrDefault(block, Collections.emptySet()).contains(spillCandidate.vr)) {
                continue;
            }

            if (blockContainsSavedRegister(block, spillCandidate.vr)) {
                continue;
            }

            List<IRInstruction> instructions = new ArrayList<>();
            for (IRInstruction inst = block.getFirst(); inst != null; inst = inst.getNext()) {
                instructions.add(inst);
            }

            List<Integer> outerUses = directUseIndices(instructions, spillCandidate.vr);
            if (outerUses.size() < 2) {
                continue;
            }

            Optional<VirtualRegister> maybeInner = candidateNeighbors.stream()
                    .filter(inner -> !blockContainsSavedRegister(block, inner))
                    .filter(inner -> !inner.equals(spillCandidate.vr))
                    .filter(inner -> isNestedWithinSameBlock(instructions, outerUses, inner))
                    .max(Comparator
                            .comparingInt((VirtualRegister vr) -> spillCost.getOrDefault(vr, 0))
                            .thenComparingInt(VirtualRegister::getRegisterNumber));

            if (maybeInner.isEmpty()) {
                continue;
            }

            VirtualRegister inner = maybeInner.orElseThrow();
            List<Integer> innerUses = directUseIndices(instructions, inner);

            int innerFirstUse = innerUses.get(0);
            int innerLastUse = innerUses.get(innerUses.size() - 1);

            int pushBeforeIndex = lastIndexBefore(outerUses, innerFirstUse);
            int popBeforeIndex = firstIndexAfter(outerUses, innerLastUse);
            if (pushBeforeIndex < 0 || popBeforeIndex < 0 || pushBeforeIndex >= popBeforeIndex) {
                continue;
            }

            if (applySameBlockSplit(unit, block, instructions, spillCandidate.vr, pushBeforeIndex, popBeforeIndex)) {
                return true;
            }
        }

        return false;
    }

    private static boolean applySameBlockSplit(IRUnit unit,
                                               IRBlock block,
                                               List<IRInstruction> instructions,
                                               VirtualRegister outer,
                                               int pushBeforeIndex,
                                               int popBeforeIndex) {
        IRInstruction pushSite = pushBeforeIndex < 0 ? block.getFirst() : instructions.get(pushBeforeIndex);
        IRInstruction popSite = instructions.get(popBeforeIndex);

        VirtualRegister restored = unit.getVirtualRegisterManager().getRegister(outer.getTypeSpecifier());
        restored.setRegisterClass(outer.getRegisterClass());

        if (pushBeforeIndex < 0) {
            pushSite.insertBefore(new Push(outer));
        } else {
            pushSite.insertAfter(new Push(outer));
        }

        Pop restore = new Pop(restored);
        if (popSite == block.getFirst()) {
            popSite.insertBefore(restore);
        } else {
            popSite.insertBefore(restore);
        }

        for (IRInstruction cursor = restore.getNext(); cursor != null; cursor = cursor.getNext()) {
            cursor.replaceOperand(outer, restored);
        }

        return true;
    }

    private static boolean isNestedWithinSameBlock(List<IRInstruction> instructions,
                                                   List<Integer> outerUses,
                                                   VirtualRegister inner) {
        List<Integer> innerUses = directUseIndices(instructions, inner);
        if (innerUses.isEmpty()) {
            return false;
        }

        return outerUses.get(0) < innerUses.get(0)
                && innerUses.get(innerUses.size() - 1) < outerUses.get(outerUses.size() - 1);
    }

    private static List<Integer> directUseIndices(List<IRInstruction> instructions, VirtualRegister vr) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < instructions.size(); i++) {
            IRInstruction inst = instructions.get(i);
            if (inst.getReads().contains(vr) || inst.getWrites().contains(vr)) {
                indices.add(i);
            }
        }
        return indices;
    }

    private static int lastIndexBefore(List<Integer> indices, int boundaryExclusive) {
        int result = -1;
        for (int index : indices) {
            if (index >= boundaryExclusive) {
                break;
            }
            result = index;
        }
        return result;
    }

    private static int firstIndexAfter(List<Integer> indices, int boundaryExclusive) {
        for (int index : indices) {
            if (index > boundaryExclusive) {
                return index;
            }
        }
        return -1;
    }

    private static boolean blockContainsSavedRegister(IRBlock block, VirtualRegister vr) {
        for (IRInstruction inst = block.getFirst(); inst != null; inst = inst.getNext()) {
            if (inst instanceof Push push && vr.equals(push.getArg())) {
                return true;
            }
            if (inst instanceof Pop pop && vr.equals(pop.getArg())) {
                return true;
            }
        }
        return false;
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
            StackFrameLocation sfOp = (StackFrameLocation) store.getDest();
            sfOp.setOffset(slotOf.get(vr));
        }

        for(var entry : spillLoads) {
            VirtualRegister vr = entry.getKey();
            Move load = entry.getValue();
            StackFrameLocation sfOp = (StackFrameLocation) load.getSource();
            sfOp.setOffset(slotOf.get(vr));
        }
    }

    public record AllocationInfo(InterferenceGraph interferenceGraph, LivenessInfo livenessInfo) {
    }
}

