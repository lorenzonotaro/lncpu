package com.lnc.cc.codegen;


import com.lnc.LNC;
import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.optimization.ir.StageOneIROptimizer;
import com.lnc.common.Logger;
import com.mxgraph.view.mxGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final boolean diagnosticsEnabled;
    private final Set<VirtualRegister> spillEligibleVirtuals;
    private final IRUnit unit;
    private final LivenessInfo livenessInfo;

    // worklists and stacks
    private Deque<InterferenceGraph.Node> selectStack = new ArrayDeque<>();
    private final List<InterferenceGraph.Node> spillCandidates = new ArrayList<>();
    private List<SpillChoice> spillChoices = List.of();
    private final Set<InterferenceGraph.Node> coloredNodes    = new LinkedHashSet<>();
    private final Set<Register> usedRegisters = new LinkedHashSet<>();

    private final Map<VirtualRegister, Long> projectedSpillCost = new LinkedHashMap<>();
    private static final long SPILL_SCORE_SCALE = 1024L;
    private static final long MAX_SPILL_COST = Long.MAX_VALUE / SPILL_SCORE_SCALE;

    // One mutable neighbor view drives simplify, selection, and spill scoring. Removed
    // nodes retain their removal-time snapshot for reverse-order coloring.
    private final Map<InterferenceGraph.Node, Set<InterferenceGraph.Node>> decisionNeighbors = new LinkedHashMap<>();
    private final Set<InterferenceGraph.Node> simplifyNodes = new LinkedHashSet<>();

    // aliasing and degree tracking
    private final Map<InterferenceGraph.Node,InterferenceGraph.Node> alias = new HashMap<>();
    private final Set<AbstractMap.SimpleEntry<InterferenceGraph.Node,InterferenceGraph.Node>> worklistMoves = new LinkedHashSet<>();

    public GraphColoringRegisterAllocator(InterferenceGraph graph,
                                          Set<VirtualRegister> spillEligibleVirtuals,
                                          IRUnit unit,
                                          LivenessInfo livenessInfo) {
        this.graph = graph;
        this.spillEligibleVirtuals = Set.copyOf(spillEligibleVirtuals);
        this.unit = unit;
        this.livenessInfo = livenessInfo;
        // count physical nodes (excluding compounds if you treat them specially)
        this.K = graph.getPhysicalNodes().size();

        computeSpillCosts();

        coloredNodes.addAll(graph.getPhysicalNodes());

        worklistMoves.addAll(graph.getMoveEdges());

        this.doCoalesce = !LNC.settings.get("--reg-alloc-no-coalesce", Boolean.class);
        this.diagnosticsEnabled = LNC.settings.get("--reg-alloc-diagnostics", Boolean.class);
    }

    private void computeSpillCosts() {
        projectedSpillCost.clear();
        for (InterferenceGraph.Node node : graph.getVirtualNodes()) {
            projectedSpillCost.put(node.vr, 1L);
        }

        for (IRBlock block : unit.computeReversePostOrderAndCFG()) {
            long loopWeight = loopExecutionWeight(block.getLoopDepth());
            for (IRInstruction inst = block.getFirst(); inst != null; inst = inst.getNext()) {
                Set<VirtualRegister> reads = new LinkedHashSet<>(inst.getReads());
                Set<VirtualRegister> writes = new LinkedHashSet<>(inst.getWrites());
                Set<VirtualRegister> touched = new LinkedHashSet<>(reads);
                touched.addAll(writes);

                for (VirtualRegister vr : touched) {
                    InterferenceGraph.Node node = getAlias(graph.getNode(vr));
                    int expectedTraffic = (reads.contains(vr) ? 1 : 0)
                            + (writes.contains(vr) && livenessInfo.isLiveAfter(vr, inst) ? 1 : 0);
                    if (expectedTraffic == 0) {
                        continue;
                    }

                    int pressureExcess = Math.max(
                            0,
                            compatibleLivePressure(node, inst) - effectivePaletteBudget(node) + 1
                    );
                    long siteCost = boundedMultiply(loopWeight, expectedTraffic * (long) (pressureExcess + 1));
                    projectedSpillCost.merge(vr, siteCost, GraphColoringRegisterAllocator::boundedAdd);
                }
            }
        }
    }

    private int compatibleLivePressure(InterferenceGraph.Node node, IRInstruction inst) {
        Set<VirtualRegister> liveAtTouch = new LinkedHashSet<>(livenessInfo.getLiveAfter(inst));
        liveAtTouch.addAll(inst.getReads());
        liveAtTouch.addAll(inst.getWrites());
        liveAtTouch.remove(node.vr);

        int pressure = 0;
        for (VirtualRegister liveVr : liveAtTouch) {
            InterferenceGraph.Node liveNode = getAlias(graph.getNode(liveVr));
            if (liveNode != node && palettesOverlap(node, liveNode)) {
                pressure += overlappingNeighborPressure(node, liveNode);
            }
        }
        return pressure;
    }

    private static long loopExecutionWeight(int loopDepth) {
        long weight = 1;
        for (int i = 0; i < loopDepth; i++) {
            weight = boundedMultiply(weight, 10);
        }
        return weight;
    }

    private static long boundedMultiply(long left, long right) {
        if (left <= 0 || right <= 0) {
            return 0;
        }
        return left > MAX_SPILL_COST / right ? MAX_SPILL_COST : left * right;
    }

    private static long boundedAdd(long left, long right) {
        return left >= MAX_SPILL_COST - right ? MAX_SPILL_COST : left + right;
    }

    public void allocate() {
        spillCandidates.clear();
        // Initialize the simplify work graph
        buildDecisionGraph();
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
                        buildDecisionGraph();
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

            if (isSpillEligible(x) != isSpillEligible(y) && !x.isPhysical() && !y.isPhysical()) {
                it.remove();
                continue;
            }

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
        Register c = palette.stream().sorted().findFirst().orElseThrow();

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
                    Register uc = u.allowedColors().stream().sorted().findFirst().orElseThrow();
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

    private void buildDecisionGraph() {
        decisionNeighbors.clear();
        simplifyNodes.clear();
        for (var n0 : graph.getVirtualNodes()) {
            var n = getAlias(n0); // ensure we consider the current representative
            if (decisionNeighbors.containsKey(n))
                continue;
            Set<InterferenceGraph.Node> nbrs =
                    n.adj.stream()
                            .map(this::getAlias)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            // remove potential self-edges created by aliasing
            nbrs.remove(n);
            decisionNeighbors.put(n, new LinkedHashSet<>(nbrs));
            simplifyNodes.add(n);
        }
    }

    private boolean simplifyStep() {
        if (simplifyNodes.isEmpty()) return false;

        // Prefer a trivially-colorable node; otherwise choose a spill candidate
        Optional<InterferenceGraph.Node> maybe =
                simplifyNodes.stream()
                        .filter(n -> decisionNeighbors.get(n).size() < n.allowedColors().size())
                        .max(Comparator.comparingInt(n -> n.allowedColors().size()));

        InterferenceGraph.Node n = maybe.orElseGet(() -> simplifyNodes.stream()
                .filter(this::isSpillEligible)
                .min(spillSelectionComparator())
                .orElseGet(() -> simplifyNodes.stream()
                        .min(Comparator.comparingInt(node -> node.vr.getRegisterNumber()))
                        .orElseThrow()));

        for (var neighbor : decisionNeighbors.get(n)) {
            if (simplifyNodes.contains(neighbor)) {
                decisionNeighbors.get(neighbor).remove(n);
            }
        }
        simplifyNodes.remove(n);
        selectStack.push(n);
        return true;
    }
    private void select() {
        while (!selectStack.isEmpty()) {
            var n = selectStack.pop();

            // Collect assigned colors from already-colored neighbors.
            Set<Register> usedNeighborColors = new LinkedHashSet<>();
            for (var w : allocationNeighbors(n)) {
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
                spillCandidates.add(spillCandidate);
                return;
            }
        }
    }

    private InterferenceGraph.Node chooseSpillVictim(InterferenceGraph.Node failedNode,
                                                     Set<Register> usedNeighborColors) {
        Set<InterferenceGraph.Node> blockers = allocationNeighbors(failedNode).stream()
                .map(this::getAlias)
                .filter(neighbor -> neighbor != failedNode)
                .filter(coloredNodes::contains)
                .filter(neighbor -> neighbor.assigned != null)
                .filter(neighbor -> usedNeighborColors.stream().anyMatch(used -> overlaps(used, neighbor.assigned)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Comparator<InterferenceGraph.Node> ranking = blockerSelectionComparator(failedNode, blockers);
        InterferenceGraph.Node bestBlocker = blockers.stream()
                .filter(this::isSpillEligible)
                .min(ranking)
                .orElse(null);

        InterferenceGraph.Node selected;
        if (!isSpillEligible(failedNode)) {
            if (bestBlocker == null) {
                throw noEligibleSpillCandidate(failedNode);
            }
            selected = bestBlocker;
        } else if (bestBlocker != null
                && freesTargetCapacity(failedNode, bestBlocker, blockers)
                && blockerSelectionScore(failedNode, bestBlocker, blockers) < spillSelectionScore(failedNode)) {
            selected = bestBlocker;
        } else {
            selected = failedNode;
        }

        logSpillDecision(failedNode, blockers, selected);
        LinkedHashMap<InterferenceGraph.Node, SpillChoice> choices = new LinkedHashMap<>();
        choices.put(getAlias(selected), spillChoice(selected));
        List<InterferenceGraph.Node> alternatives = new ArrayList<>(blockers);
        alternatives.add(failedNode);
        alternatives.stream()
                .filter(this::isSpillEligible)
                .map(this::getAlias)
                .filter(this::isSpillEligible)
                .sorted(Comparator.comparingInt(this::groupFirstRegisterNumber))
                .forEach(candidate -> choices.putIfAbsent(candidate, spillChoice(candidate)));
        spillChoices = List.copyOf(choices.values());
        return selected;
    }

    private SpillChoice spillChoice(InterferenceGraph.Node node) {
        return new SpillChoice(aliasGroup(node).stream()
                .map(member -> member.vr.getRegisterNumber())
                .sorted()
                .toList());
    }

    private boolean isSpillEligible(InterferenceGraph.Node node) {
        return node != null
                && !node.isPhysical()
                && node.vr != null
                && spillEligibleVirtuals.contains(node.vr);
    }

    private static IllegalStateException noEligibleSpillCandidate(InterferenceGraph.Node failedNode) {
        return new IllegalStateException(
                "Register allocation has no eligible spill candidate for uncolorable node " + failedNode
        );
    }

    private Comparator<InterferenceGraph.Node> spillSelectionComparator() {
        return Comparator
                .comparingLong(this::spillSelectionScore)
                .thenComparing(Comparator.comparingInt(this::groupPressureRelief).reversed())
                .thenComparingLong(this::groupSpillCost)
                .thenComparingInt(this::groupMovePartners)
                .thenComparingInt(this::groupFirstRegisterNumber);
    }

    private long spillSelectionScore(InterferenceGraph.Node n) {
        if (n == null || n.isPhysical() || n.vr == null) {
            return Long.MAX_VALUE;
        }

        return Math.max(
                1L,
                boundedMultiply(groupSpillCost(n), SPILL_SCORE_SCALE) / Math.max(1, groupPressureRelief(n))
        );
    }

    private long groupSpillCost(InterferenceGraph.Node n) {
        long cost = 0;
        for (InterferenceGraph.Node member : aliasGroup(n)) {
            cost = boundedAdd(cost, projectedSpillCost.getOrDefault(member.vr, MAX_SPILL_COST));
        }
        return Math.max(1, cost);
    }

    private int groupPressureRelief(InterferenceGraph.Node n) {
        Set<InterferenceGraph.Node> ranges = aliasGroup(n).stream()
                .map(this::getAlias)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return ranges.stream().mapToInt(this::rangePressureRelief).sum();
    }

    private int groupMovePartners(InterferenceGraph.Node n) {
        return aliasGroup(n).stream().mapToInt(member -> member.movePartners.size()).sum();
    }

    private int groupFirstRegisterNumber(InterferenceGraph.Node n) {
        return aliasGroup(n).stream()
                .mapToInt(member -> member.vr.getRegisterNumber())
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private List<InterferenceGraph.Node> aliasGroup(InterferenceGraph.Node n) {
        InterferenceGraph.Node representative = getAlias(n);
        return graph.getVirtualNodes().stream()
                .filter(member -> getAlias(member) == representative)
                .sorted(Comparator.comparingInt(member -> member.vr.getRegisterNumber()))
                .toList();
    }

    private int rangePressureRelief(InterferenceGraph.Node n) {
        if (n == null || n.isPhysical()) {
            return 1;
        }

        int relief = Math.max(1, ownPaletteExcess(n) * classScarcityWeight(n));
        for (InterferenceGraph.Node neighbor : allocationNeighbors(n)) {
            neighbor = getAlias(neighbor);
            if (neighbor == n || neighbor.isPhysical() || neighbor.vr == null || !palettesOverlap(n, neighbor)) {
                continue;
            }

            int before = effectivePaletteDegree(neighbor);
            int after = Math.max(0, before - overlappingNeighborPressure(neighbor, n));
            int budget = effectivePaletteBudget(neighbor);
            int beforeExcess = Math.max(0, before - budget + 1);
            int afterExcess = Math.max(0, after - budget + 1);
            int neighborRelief = Math.max(0, beforeExcess - afterExcess);

            if (neighborRelief == 0 && before >= budget && after < before) {
                neighborRelief = 1;
            }

            relief += neighborRelief * classScarcityWeight(neighbor);
        }

        return Math.max(1, relief);
    }

    private Comparator<InterferenceGraph.Node> blockerSelectionComparator(
            InterferenceGraph.Node failedNode,
            Set<InterferenceGraph.Node> blockers
    ) {
        return Comparator
                .comparingInt(
                        (InterferenceGraph.Node candidate) -> usableCapacityFreed(failedNode, candidate, blockers)
                ).reversed()
                .thenComparing(Comparator.comparingInt(
                        (InterferenceGraph.Node candidate) -> usableComponentsFreed(failedNode, candidate, blockers)
                ).reversed())
                .thenComparingLong(candidate -> blockerSelectionScore(failedNode, candidate, blockers))
                .thenComparing(spillSelectionComparator());
    }

    private boolean freesTargetCapacity(InterferenceGraph.Node failedNode,
                                        InterferenceGraph.Node candidate,
                                        Set<InterferenceGraph.Node> blockers) {
        return usableCapacityFreed(failedNode, candidate, blockers) > 0
                || usableComponentsFreed(failedNode, candidate, blockers) > 0;
    }

    private long blockerSelectionScore(InterferenceGraph.Node failedNode,
                                       InterferenceGraph.Node candidate,
                                       Set<InterferenceGraph.Node> blockers) {
        int targetRelief = usableCapacityFreed(failedNode, candidate, blockers)
                * classScarcityWeight(failedNode)
                + usableComponentsFreed(failedNode, candidate, blockers);
        int relief = Math.max(1, groupPressureRelief(candidate) + targetRelief);
        return Math.max(1L, boundedMultiply(groupSpillCost(candidate), SPILL_SCORE_SCALE) / relief);
    }

    private int usableCapacityFreed(InterferenceGraph.Node failedNode,
                                    InterferenceGraph.Node candidate,
                                    Set<InterferenceGraph.Node> blockers) {
        int before = maxNonOverlappingColors(availableColors(failedNode, null, blockers));
        int after = maxNonOverlappingColors(availableColors(failedNode, getAlias(candidate), blockers));
        return Math.max(0, after - before);
    }

    private Set<Register> availableColors(InterferenceGraph.Node failedNode,
                                          InterferenceGraph.Node removed,
                                          Set<InterferenceGraph.Node> blockers) {
        return failedNode.allowedColors().stream()
                .filter(color -> blockers.stream()
                        .filter(blocker -> getAlias(blocker) != removed)
                        .noneMatch(blocker -> overlaps(color, blocker.assigned)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int usableComponentsFreed(InterferenceGraph.Node failedNode,
                                      InterferenceGraph.Node candidate,
                                      Set<InterferenceGraph.Node> blockers) {
        Set<Register> relevantComponents = failedNode.allowedColors().stream()
                .flatMap(color -> Arrays.stream(color.getComponents()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Register> blockedBefore = blockedComponents(relevantComponents, null, blockers);
        Set<Register> blockedAfter = blockedComponents(relevantComponents, getAlias(candidate), blockers);
        blockedBefore.removeAll(blockedAfter);
        return blockedBefore.size();
    }

    private Set<Register> blockedComponents(Set<Register> relevantComponents,
                                            InterferenceGraph.Node removed,
                                            Set<InterferenceGraph.Node> blockers) {
        return blockers.stream()
                .filter(blocker -> getAlias(blocker) != removed)
                .flatMap(blocker -> Arrays.stream(blocker.assigned.getComponents()))
                .filter(relevantComponents::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void logSpillDecision(InterferenceGraph.Node failedNode,
                                  Set<InterferenceGraph.Node> blockers,
                                  InterferenceGraph.Node selected) {
        if (!diagnosticsEnabled) {
            return;
        }

        Logger.out("reg-alloc: failed=%s palette=%s".formatted(formatVirtual(failedNode), formatPalette(failedNode)));
        Set<InterferenceGraph.Node> candidates = blockers.stream()
                .filter(this::isSpillEligible)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (isSpillEligible(failedNode)) {
            candidates.add(failedNode);
        }

        candidates.stream()
                .sorted(Comparator.comparingInt(this::groupFirstRegisterNumber))
                .forEach(candidate -> {
                    boolean isBlocker = blockers.contains(getAlias(candidate));
                    int capacity = isBlocker ? usableCapacityFreed(failedNode, candidate, blockers) : 0;
                    int components = isBlocker ? usableComponentsFreed(failedNode, candidate, blockers) : 0;
                    int relief = groupPressureRelief(candidate)
                            + capacity * classScarcityWeight(failedNode)
                            + components;
                    long score = isBlocker
                            ? blockerSelectionScore(failedNode, candidate, blockers)
                            : spillSelectionScore(candidate);
                    Logger.out(
                            "reg-alloc: candidate=%s palette=%s cost=%d relief=%d capacity=%d components=%d score=%d selected=%s"
                                    .formatted(
                                            formatAliasGroup(candidate),
                                            formatPalette(candidate),
                                            groupSpillCost(candidate),
                                            relief,
                                            capacity,
                                            components,
                                            score,
                                            getAlias(candidate) == getAlias(selected)
                                    )
                    );
                });
        Logger.out("reg-alloc: selected=%s".formatted(formatAliasGroup(selected)));
    }

    private String formatAliasGroup(InterferenceGraph.Node node) {
        return aliasGroup(node).stream()
                .map(GraphColoringRegisterAllocator::formatVirtual)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String formatVirtual(InterferenceGraph.Node node) {
        return "r" + node.vr.getRegisterNumber();
    }

    private static String formatPalette(InterferenceGraph.Node node) {
        return node.allowedColors().stream()
                .sorted()
                .map(Register::name)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private int ownPaletteExcess(InterferenceGraph.Node n) {
        return Math.max(1, effectivePaletteDegree(n) - effectivePaletteBudget(n) + 1);
    }

    private int effectivePaletteDegree(InterferenceGraph.Node n) {
        Set<Register> fixedPressure = new LinkedHashSet<>();
        int virtualPressure = 0;

        for (InterferenceGraph.Node neighbor : allocationNeighbors(n)) {
            neighbor = getAlias(neighbor);
            if (neighbor == n || !palettesOverlap(n, neighbor)) {
                continue;
            }

            if (neighbor.isPhysical() || neighbor.isPseudoPhysical()) {
                for (Register color : neighbor.allowedColors()) {
                    if (paletteContainsOverlap(n.allowedColors(), color)) {
                        fixedPressure.add(color);
                    }
                }
            } else {
                virtualPressure++;
            }
        }

        return virtualPressure + maxNonOverlappingColors(fixedPressure);
    }

    private int effectivePaletteBudget(InterferenceGraph.Node n) {
        return Math.max(1, maxNonOverlappingColors(n.allowedColors()));
    }

    private int overlappingNeighborPressure(InterferenceGraph.Node target, InterferenceGraph.Node neighbor) {
        if (!palettesOverlap(target, neighbor)) {
            return 0;
        }
        if (neighbor.isPhysical() || neighbor.isPseudoPhysical()) {
            Set<Register> overlappingColors = neighbor.allowedColors().stream()
                    .filter(color -> paletteContainsOverlap(target.allowedColors(), color))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return Math.max(1, maxNonOverlappingColors(overlappingColors));
        }
        return 1;
    }

    private int classScarcityWeight(InterferenceGraph.Node n) {
        int budget = effectivePaletteBudget(n);
        return Math.max(1, (maxPaletteBudget() * paletteRegisterWidth(n)) / Math.max(1, budget));
    }

    private int maxPaletteBudget() {
        return graph.getVirtualNodes().stream()
                .map(this::getAlias)
                .mapToInt(this::effectivePaletteBudget)
                .max()
                .orElse(Math.max(1, K));
    }

    private int paletteRegisterWidth(InterferenceGraph.Node n) {
        return n.allowedColors().stream()
                .mapToInt(color -> Math.max(1, color.getComponents().length))
                .max()
                .orElse(1);
    }

    private Set<InterferenceGraph.Node> allocationNeighbors(InterferenceGraph.Node n) {
        return decisionNeighbors.getOrDefault(getAlias(n), Collections.emptySet()).stream()
                .map(this::getAlias)
                .filter(neighbor -> neighbor != n)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean palettesOverlap(InterferenceGraph.Node a, InterferenceGraph.Node b) {
        for (Register ac : a.allowedColors()) {
            if (paletteContainsOverlap(b.allowedColors(), ac)) {
                return true;
            }
        }
        return false;
    }

    private static boolean paletteContainsOverlap(Set<Register> palette, Register color) {
        return palette.stream().anyMatch(candidate -> overlaps(candidate, color));
    }

    private static int maxNonOverlappingColors(Collection<Register> colors) {
        List<Register> ordered = colors.stream().sorted().toList();
        return maxNonOverlappingColors(ordered, 0, new ArrayList<>());
    }

    private static int maxNonOverlappingColors(List<Register> colors, int index, List<Register> chosen) {
        if (index >= colors.size()) {
            return chosen.size();
        }

        int best = maxNonOverlappingColors(colors, index + 1, chosen);
        Register candidate = colors.get(index);
        boolean canUse = chosen.stream().noneMatch(existing -> overlaps(existing, candidate));
        if (canUse) {
            chosen.add(candidate);
            best = Math.max(best, maxNonOverlappingColors(colors, index + 1, chosen));
            chosen.remove(chosen.size() - 1);
        }
        return best;
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
                    .max(Map.Entry.<Register, Integer>comparingByValue()
                            .thenComparing((e1, e2) -> e1.getKey().name().compareTo(e2.getKey().name())))
                    .orElseThrow()
                    .getKey();
        }

        // 2) Fallback: pick the first available color deterministically (by register name order).
        return okColors.stream().sorted().findFirst().orElseThrow();
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

    Set<Register> getUsedRegisters() {
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
        if (!isSpillEligible(rep)) {
            throw new IllegalStateException("Register allocator selected an ineligible spill candidate " + rep);
        }
        Set<VirtualRegister> spilled = new LinkedHashSet<>();
        for (InterferenceGraph.Node n : graph.getVirtualNodes()) {
            if (getAlias(n) == rep) {
                if (!isSpillEligible(n)) {
                    throw new IllegalStateException(
                            "Register allocator aliased spill-eligible and protected nodes through " + rep
                    );
                }
                spilled.add(n.vr);
            }
        }
        return spilled;
    }

    public InterferenceGraph.Node getSpillCandidate() {
        return spillCandidates.stream().filter(Objects::nonNull).min(
                spillSelectionComparator()).orElse(null);
    }

    List<SpillChoice> getSpillChoices() {
        return spillChoices;
    }

    Set<VirtualRegister> getSpilledVirtualRegisters(SpillChoice choice) {
        if (!spillChoices.contains(choice) || choice.registerNumbers().isEmpty()) {
            throw new IllegalArgumentException("Spill choice is not eligible for this allocation failure");
        }
        VirtualRegister first = graph.getVirtualNodes().stream()
                .map(node -> node.vr)
                .filter(Objects::nonNull)
                .filter(register -> register.getRegisterNumber() == choice.registerNumbers().get(0))
                .findFirst()
                .orElseThrow();
        return getSpilledVirtualRegisters(graph.getNode(first));
    }

    public record SpillChoice(List<Integer> registerNumbers) {
        public SpillChoice {
            registerNumbers = List.copyOf(registerNumbers);
        }
    }


    public static AllocationInfo run(IRUnit unit){
        return RegisterAllocationDriver.run(unit);
    }

    static void applySpillChoice(IRUnit unit,
                                 LivenessInfo livenessInfo,
                                 InterferenceGraph graph,
                                 Set<VirtualRegister> spilledVirtuals,
                                 SpillSlotAssigner slotAssigner) {
        List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillStores = new ArrayList<>();
        List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillLoads = new ArrayList<>();
        for (IRBlock block : unit.computeReversePostOrderAndCFG()) {
            for (IRInstruction instruction = block.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof Move move && move.isRegParamDemotion())) continue;
                for (VirtualRegister register : spilledVirtuals) {
                    if (!register.equals(move.getDest())) continue;
                    move.setDest(new StackFrameLocation(register.getTypeSpecifier(), StackFrameLocation.OperandType.LOCAL, 0));
                    spillStores.add(new AbstractMap.SimpleEntry<>(register, move));
                }
            }
        }
        Map<VirtualRegister, List<SpillSegment>> segments = planSpillSegments(unit, livenessInfo, graph, spilledVirtuals);
        for (VirtualRegister register : spilledVirtuals) {
            for (SpillSegment segment : segments.getOrDefault(register, Collections.emptyList())) {
                applySpillSegment(unit, register, segment, livenessInfo, spillStores, spillLoads);
            }
        }
        Map<VirtualRegister, LiveRange> spillRanges = graph.getLiveRanges().entrySet().stream()
                .filter(entry -> spilledVirtuals.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        slotAssigner.assignSlots(spillRanges);
        patchSpillOffsets(
                spillStores,
                spillLoads,
                slotAssigner.slotOffset,
                unit.getLocalMappingInfo().forcedStackFrameLocalsSize()
        );
        maybeRunPostSpillIROptimizer(unit);
    }

    static Set<VirtualRegister> constrainedCallReturnTargets(IRUnit unit) {
        Set<VirtualRegister> constrainedReturns = new LinkedHashSet<>();
        for (IRBlock block : unit.computeReversePostOrderAndCFG()) {
            for (IRInstruction inst = block.getFirst(); inst != null; inst = inst.getNext()) {
                if (!(inst instanceof Call call) || call.getReturnTarget() == null) {
                    continue;
                }

                VirtualRegister returnTarget = call.getReturnTarget();
                RegisterClass unconstrainedClass = returnTarget.getTypeSpecifier().allocSize() == 1
                        ? RegisterClass.ANY
                        : RegisterClass.WORD;
                if (!returnTarget.getRegisterClass().equals(unconstrainedClass)) {
                    constrainedReturns.add(returnTarget);
                }
            }
        }
        return constrainedReturns;
    }

    static void clearRegAllocIterFiles(IRUnit unit) {
        if(!LNC.settings.get("--output-reg-alloc-iter", String.class).isEmpty()){
            // Clear files matching the unit prefix in the folder;
            Path dir = Path.of(LNC.settings.get("--output-reg-alloc-iter", String.class));
            if(Files.isDirectory(dir)) {
                for (File file : Objects.requireNonNull(dir.toFile().listFiles())) {
                    if (file.getName().startsWith(unit.getFunctionDeclaration().name.lexeme)) {
                        file.delete();
                    }
                }
            }
        }
    }

    static void saveStep(IRUnit unit, int i, InterferenceGraph ig, InterferenceGraph.Node previousSpilledCandidate, String outputRegAllocPath) {
        Path path = Path.of(outputRegAllocPath);
        if(!(Files.exists(path) && Files.isDirectory(path))){
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                Logger.error("Failed to create directory for output reg alloc path: " + outputRegAllocPath);
                LNC.settings.set("--output-reg-alloc-iter", ""); // reset the flag
            }
        }
        var irPrinter = new IRPrinter();

        irPrinter.visit(unit);

        try {
            StringBuilder sb = new StringBuilder();

            sb.append("#### IR of register allocation n°").append(i).append(" for unit ").append(unit.getFunctionDeclaration().name.lexeme).append(" ####\n\n");

            if(previousSpilledCandidate != null) {
                sb.append("#### Previous iteration chose to spill the following register: ").append(previousSpilledCandidate.vr).append("\n\n");
            }

            if(ig != null) {
                sb.append("#### Interference graph at the start of this iteration: ####\n\n");
                sb.append(ig).append("\n\n");
            }

            sb.append(irPrinter.getResult());
            Files.writeString(Path.of(outputRegAllocPath, String.format("%s_%d.immediate.txt", unit.getFunctionDeclaration().name.lexeme, i)),
                    sb.toString());
        } catch (IOException e) {
            Logger.error("Failed to write IR to file: " + e.getMessage());
        }
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

                for (int touchIndex : touchIndices) {
                    segments.add(new SpillSegment(block, instructions, touchIndex, touchIndex));
                }
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
                replaceSpilledVirtualRegister(inst, vr, temp);
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

    private static void replaceSpilledVirtualRegister(IRInstruction inst,
                                                      VirtualRegister oldVr,
                                                      VirtualRegister newVr) {
        // Some instructions can both read and write the same operand. A second pass
        // lets existing replaceOperand implementations update the write after the read.
        inst.replaceOperand(oldVr, newVr);
        inst.replaceOperand(oldVr, newVr);

        for (IROperand operand : inst.getReadOperands()) {
            replaceNestedVirtualRegister(operand, oldVr, newVr);
        }
        replaceNestedVirtualRegisterInWriteOperands(inst, oldVr, newVr);
    }

    private static void replaceNestedVirtualRegisterInWriteOperands(IRInstruction inst,
                                                                    VirtualRegister oldVr,
                                                                    VirtualRegister newVr) {
        if (inst instanceof Move move) {
            replaceNestedVirtualRegister(move.getDest(), oldVr, newVr);
        } else if (inst instanceof Bin bin) {
            replaceNestedVirtualRegister(bin.getDest(), oldVr, newVr);
        } else if (inst instanceof Unary unary) {
            replaceNestedVirtualRegister(unary.getTarget(), oldVr, newVr);
        } else if (inst instanceof Pop pop) {
            replaceNestedVirtualRegister(pop.getArg(), oldVr, newVr);
        } else if (inst instanceof Call call && oldVr.equals(call.getReturnTarget())) {
            call.setReturnTarget(newVr);
        }
    }

    private static void replaceNestedVirtualRegister(IROperand operand,
                                                     VirtualRegister oldVr,
                                                     VirtualRegister newVr) {
        if (operand instanceof DerefLocation deref) {
            if (deref.getTarget().equals(oldVr)) {
                deref.setTarget(newVr);
            } else {
                replaceNestedVirtualRegister(deref.getTarget(), oldVr, newVr);
            }
            return;
        }

        if (operand instanceof SizedCast cast) {
            if (cast.getOperand().equals(oldVr)) {
                cast.setOperand(newVr);
            } else {
                replaceNestedVirtualRegister(cast.getOperand(), oldVr, newVr);
            }
            return;
        }

        if (operand instanceof ComposeOperand compose) {
            if (compose.high.equals(oldVr)) {
                compose.high = newVr;
            } else {
                replaceNestedVirtualRegister(compose.high, oldVr, newVr);
            }

            if (compose.low.equals(oldVr)) {
                compose.low = newVr;
            } else {
                replaceNestedVirtualRegister(compose.low, oldVr, newVr);
            }
            return;
        }

        if (operand instanceof AddressOf addressOf) {
            replaceNestedVirtualRegister(addressOf.getOperand(), oldVr, newVr);
            return;
        }

        if (operand instanceof StructMemberAccess memberAccess) {
            replaceNestedVirtualRegister(memberAccess.getBase(), oldVr, newVr);
            return;
        }

        if (operand instanceof ArrayIndexLocation arrayIndex) {
            replaceNestedVirtualRegister(arrayIndex.getBase(), oldVr, newVr);
            replaceNestedVirtualRegister(arrayIndex.getIndex(), oldVr, newVr);
        }
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

    private static void patchSpillOffsets(List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillStores, List<AbstractMap.SimpleEntry<VirtualRegister, Move>> spillLoads, Map<VirtualRegister, Integer> slotOf, int spillBaseOffset) {
        for(var entry : spillStores) {
            VirtualRegister vr = entry.getKey();
            Move store = entry.getValue();
            StackFrameLocation sfOp = (StackFrameLocation) store.getDest();
            sfOp.setOffset(spillBaseOffset + slotOf.get(vr));
        }

        for(var entry : spillLoads) {
            VirtualRegister vr = entry.getKey();
            Move load = entry.getValue();
            StackFrameLocation sfOp = (StackFrameLocation) load.getSource();
            sfOp.setOffset(spillBaseOffset + slotOf.get(vr));
        }
    }

    public record TerminalMetric(boolean feasible,
                                 long loopWeightedSpillTraffic,
                                 int spillInstructions,
                                 int spillSlots,
                                 int frameSpillSize,
                                 int spillRounds,
                                 List<List<Integer>> spillSequence) implements Comparable<TerminalMetric> {
        public TerminalMetric {
            spillSequence = spillSequence.stream().map(List::copyOf).toList();
        }

        @Override
        public int compareTo(TerminalMetric other) {
            if (feasible != other.feasible) return feasible ? -1 : 1;
            int result = Long.compare(loopWeightedSpillTraffic, other.loopWeightedSpillTraffic);
            if (result != 0) return result;
            result = Integer.compare(spillInstructions, other.spillInstructions);
            if (result != 0) return result;
            result = Integer.compare(spillSlots, other.spillSlots);
            if (result != 0) return result;
            result = Integer.compare(frameSpillSize, other.frameSpillSize);
            if (result != 0) return result;
            result = Integer.compare(spillRounds, other.spillRounds);
            if (result != 0) return result;
            int common = Math.min(spillSequence.size(), other.spillSequence.size());
            for (int index = 0; index < common; index++) {
                List<Integer> left = spillSequence.get(index);
                List<Integer> right = other.spillSequence.get(index);
                int groupCommon = Math.min(left.size(), right.size());
                for (int member = 0; member < groupCommon; member++) {
                    result = Integer.compare(left.get(member), right.get(member));
                    if (result != 0) return result;
                }
                result = Integer.compare(left.size(), right.size());
                if (result != 0) return result;
            }
            return Integer.compare(spillSequence.size(), other.spillSequence.size());
        }
    }

    public record SearchDiagnostics(int statesExplored,
                                    boolean truncatedByDepth,
                                    boolean truncatedByStates,
                                    TerminalMetric greedyMetric,
                                    TerminalMetric chosenMetric) {
    }

    public record AllocationInfo(InterferenceGraph interferenceGraph,
                                 LivenessInfo livenessInfo,
                                 TerminalMetric terminalMetric,
                                 SearchDiagnostics searchDiagnostics) {
    }
}

