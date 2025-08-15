package com.lnc.cc.codegen;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

import java.util.*;

public class InterferenceGraph {
    private final Map<VirtualRegister, Node> vregNodes   = new LinkedHashMap<>();
    private final Map<Register,        Node> physNodes   = new LinkedHashMap<>();

    // ✨ NEW: copy‑preference (move) edges. Stored as Node pairs.
    private final Set<AbstractMap.SimpleEntry<Node,Node>> moveEdges  = new LinkedHashSet<>();
    private Map<VirtualRegister, Integer> loopWeights = new LinkedHashMap<>();
    private Map<VirtualRegister, Integer> uses = new LinkedHashMap<>();
    private Map<VirtualRegister, Integer> defs = new LinkedHashMap<>();
    private Map<VirtualRegister, LiveRange> liveRanges = new LinkedHashMap<>();

    public static class Node {
        public final VirtualRegister vr;
        public final Register        phys;
        public final Set<Node>       adj = new LinkedHashSet<>();

        public Register assigned   = null;
        public Set<InterferenceGraph.Node> movePartners = new LinkedHashSet<>();

        private Node(VirtualRegister vr) {
            this.vr   = vr;
            this.phys = null;
        }

        private Node(Register phys) {
            this.vr = null;
            this.phys = phys;
            this.assigned   = phys;
        }

        public boolean isPhysical() { return phys != null; }

        public boolean isPseudoPhysical() {
            return isPhysical() || allowedColors().size() == 1;
        }
        public Set<Register> allowedColors() {
            return isPhysical()
                    ? new LinkedHashSet<>(Set.of(phys))
                    : vr.getRegisterClass().getRegisters();
        }

        public String toString() {
            return isPhysical() ? phys.toString() : vr.toString();
        }

        public int degree() {
            return adj.size();
        }

        public Register onlyColor() {
            if(!isPseudoPhysical())
                throw new IllegalStateException("Node is not a pseudo-physical node.");
            return isPhysical() ? phys : allowedColors().iterator().next();
        }
    }

    public InterferenceGraph() {
        // create a node for every phys‐reg
        for (Register r : Register.values()) {
            physNodes.put(r, new Node(r));
        }
        // compound ↔ components interfere
        for (Register r : Register.values()) {
            if (r.isCompound()) {
                Node comp = physNodes.get(r);
                for (Register sub : r.getComponents()) {
                    Node subN = physNodes.get(sub);
                    comp.adj.add(subN);
                    subN.adj.add(comp);
                }
            }
        }
    }

    public Node getNode(VirtualRegister vr) {
        return vregNodes.computeIfAbsent(vr, Node::new);
    }

    public Node getPhysicalNode(Register phys) {
        return physNodes.get(phys);
    }

    /**
     * Add interference between two vregs.
     */
    public void addEdge(VirtualRegister a, VirtualRegister b) {
        if (a == b) return;
        Node na = getNode(a), nb = getNode(b);
        na.adj.add(nb);
        nb.adj.add(na);
    }

    public void addPreference(VirtualRegister a, VirtualRegister b) {
        if (a == b) return;
        Node na = getNode(a), nb = getNode(b);
        moveEdges.add(new AbstractMap.SimpleEntry<>(na, nb));
        na.movePartners.add(nb);
        nb.movePartners.add(na);
    }
    public Set<AbstractMap.SimpleEntry<Node,Node>> getMoveEdges() { return moveEdges; }

    /**
     * Add interference between a vreg and a phys register,
     * automatically splitting compounds into their pieces.
     */
    public void addEdge(VirtualRegister vr, Register phys) {
        // if phys is a compound, forbid each of its components
        if (phys.isCompound()) {
            for (Register sub : phys.getComponents()) {
                addEdge(vr, sub);
            }
        }
        // and also forbid the compound node itself
        Node na = getNode(vr), nb = getPhysicalNode(phys);
        na.adj.add(nb);
        nb.adj.add(na);
    }

    public Collection<Node> getVirtualNodes() {
        return vregNodes.values();
    }

    public Collection<Node> getPhysicalNodes() {
        return physNodes.values();
    }

    public Map<VirtualRegister, LiveRange> getLiveRanges() {
        return liveRanges;
    }

    public record VrInfo(
            Map<VirtualRegister, LiveRange> liveRanges,
            Map<VirtualRegister, Integer> loopWeights,
            Map<VirtualRegister, Integer> uses,
            Map<VirtualRegister, Integer> defs
    ) {}

    public static VrInfo computeVrInfo(
            IRUnit unit,
            LivenessInfo li
    ) {
        // 1) create empty ranges
        Map<VirtualRegister,LiveRange> ranges = new LinkedHashMap<>();
        var uses = new LinkedHashMap<VirtualRegister, Integer>();
        var defs = new LinkedHashMap<VirtualRegister, Integer>();
        var loopWeights = new LinkedHashMap<VirtualRegister, Integer>();

        for (VirtualRegister vr : unit.getVrManager().getAllRegisters())
            ranges.put(vr, new LiveRange());

        // 2) number instructions in forward RPO
        List<IRBlock> rpo = unit.computeReversePostOrderAndCFG();
        int idx = 0;
        for (IRBlock B : rpo) {
            for (IRInstruction inst = B.getFirst(); inst != null; inst = inst.getNext()) {
                inst.setIndex(idx++);
            }
        }

        // Heuristic weights based on loop depth; also record param defs at entry
        IRBlock presumedEntry = rpo.isEmpty() ? null : rpo.get(0);
        if (presumedEntry != null) {
            for (var param : unit.getFunctionType().getParameterMapping()) {
                if (!param.onStack()) {
                    var vrParam = unit.getLocalMappingInfo().originalRegParamMappings().get(param.name());
                    if (vrParam != null) {
                        ranges.get(vrParam).addPoint(presumedEntry, 0);
                    }
                }
            }
        }

        // 3) backward scan per block, seeded with liveOut
        for (IRBlock B : rpo) {
            var blockLoopWeight = B.getLoopDepth();
            Set<VirtualRegister> live = new LinkedHashSet<>(li.liveOut().get(B));
            for (IRInstruction inst = B.getLast(); inst != null; inst = inst.getPrev()) {
                int i = inst.getIndex();

                // Compute liveBefore = (live - defs) ∪ uses
                Set<VirtualRegister> defsHere = new LinkedHashSet<>(inst.getWrites());
                Set<VirtualRegister> usesHere = new LinkedHashSet<>(inst.getReads());

                // update counts and weights
                for (VirtualRegister d : defsHere) {
                    defs.put(d, defs.getOrDefault(d, 0) + 1);
                    loopWeights.put(d, Math.max(loopWeights.getOrDefault(d, 0), (int) Math.pow(10, blockLoopWeight)));
                }
                for (VirtualRegister u : usesHere) {
                    uses.put(u, uses.getOrDefault(u, 0) + 1);
                    loopWeights.put(u, Math.max(loopWeights.getOrDefault(u, 0), (int) Math.pow(10, blockLoopWeight)));
                }

                Set<VirtualRegister> liveBefore = new LinkedHashSet<>(live);
                liveBefore.removeAll(defsHere);
                liveBefore.addAll(usesHere);

                // Mark liveness at this program point
                for (VirtualRegister v : liveBefore) {
                    LiveRange lr = ranges.get(v);
                    if (lr != null) lr.addPoint(B, i);
                }

                // Also mark direct defs/uses points for better visualization
                for (VirtualRegister d : defsHere) {
                    LiveRange lr = ranges.get(d);
                    if (lr != null) lr.addPoint(B, i);
                }
                for (VirtualRegister u : usesHere) {
                    LiveRange lr = ranges.get(u);
                    if (lr != null) lr.addPoint(B, i);
                }

                // Prepare for previous instruction
                live = liveBefore;
            }
        }

        return new VrInfo(
                ranges,
                loopWeights,
                uses,
                defs
        );
    }

    public static InterferenceGraph buildInterferenceGraph(IRUnit unit) {
        InterferenceGraph graph = new InterferenceGraph();

        // Ensure every vreg has a node, even if it never interferes
        for (VirtualRegister vr : unit.getVirtualRegisterManager().getAllRegisters()) {
            graph.getNode(vr);
        }

        // 0) compute liveness info
        LivenessInfo livenessInfo = LivenessInfo.computeBlockLiveness(unit);

        // 1) compute live ranges and weights/counters (for debugging/heuristics)
        VrInfo vrInfo = computeVrInfo(unit, livenessInfo);

        Map<VirtualRegister, LiveRange> liveRanges = vrInfo.liveRanges();
        Map<VirtualRegister, Integer> loopWeights = vrInfo.loopWeights();
        Map<VirtualRegister, Integer> uses = vrInfo.uses();
        Map<VirtualRegister, Integer> defs = vrInfo.defs();

        graph.setLiveRanges(liveRanges);
        graph.setLoopWeights(loopWeights);
        graph.setUses(uses);
        graph.setDefs(defs);

        // 2) Build interference edges in a control-flow aware manner:
        //    For each block, scan instructions backward. For each def, add edges to all currently live vregs.
        //    Special-case moves: do not connect dest with src, but add a preference instead.
        var liveOut = livenessInfo.liveOut();
        List<IRBlock> order = unit.computeReversePostOrderAndCFG();

        for (IRBlock B : order) {
            Set<VirtualRegister> live = new LinkedHashSet<>(liveOut.get(B));

            for (IRInstruction inst = B.getLast(); inst != null; inst = inst.getPrev()) {
                // Collect defs/uses
                Set<VirtualRegister> defsHere = new LinkedHashSet<>(inst.getWrites());
                Set<VirtualRegister> usesHere = new LinkedHashSet<>(inst.getReads());

                // Handle move preferences and compute "work" live set for interference
                Set<VirtualRegister> work = new LinkedHashSet<>(live);
                if (inst instanceof Move mv) {
                    IROperand s = mv.getSource(), d = mv.getDest();
                    if (s instanceof VirtualRegister vs && d instanceof VirtualRegister vd) {
                        graph.addPreference(vs, vd);
                        work.remove(vs); // do not create interference dest<->src for moves
                    }
                }

                // Add interference for defs against current live (or live minus src for moves)
                for (VirtualRegister d : defsHere) {
                    for (VirtualRegister v : work) {
                        graph.addEdge(d, v);
                    }
                }

                // Call-site clobbers: any vreg live across the call cannot take those phys colors
                if (inst instanceof Call call) {
                    var ret = call.getReturnTarget();
                    if (ret != null) {
                        for (VirtualRegister vr : live) {
                            graph.addEdge(vr, ret);
                        }
                    }
                }

                // Keep and extend copy/coalesce preferences as in the prior logic
                if (inst instanceof Bin bin) {
                    VirtualRegister dest = (VirtualRegister) bin.getDest();
                    VirtualRegister rhs = bin.getRight().type == IROperand.Type.VIRTUAL_REGISTER ? (VirtualRegister) bin.getRight() : null;
                    VirtualRegister lhs = bin.getLeft().type == IROperand.Type.VIRTUAL_REGISTER ? (VirtualRegister) bin.getLeft() : null;
                    if (lhs != null)
                        graph.addPreference(dest, lhs);
                    if (rhs != null) {
                        if (bin.getOperator().isCommutative()) {
                            graph.addPreference(dest, rhs);
                        } else {
                            // Non-commutative two-address style often requires dest ≠ rhs
                            graph.addEdge(dest, rhs);
                        }
                    }
                } else if (inst instanceof Unary un) {
                    VirtualRegister dest = (VirtualRegister) un.getTarget();
                    VirtualRegister src = un.getOperand().type == IROperand.Type.VIRTUAL_REGISTER ? (VirtualRegister) un.getOperand() : null;
                    if (src != null)
                        graph.addPreference(dest, src);
                }

                // Standard liveness update for backward scan
                live.removeAll(defsHere);
                live.addAll(usesHere);
            }
        }

        return graph;
    }

    private void setLiveRanges(Map<VirtualRegister, LiveRange> liveRanges) {
        this.liveRanges = liveRanges;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("InterferenceGraph:\n");

        if(liveRanges != null && !liveRanges.isEmpty()) {
            sb.append("Live Ranges:\n");
            for (Map.Entry<VirtualRegister, LiveRange> entry : liveRanges.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ");
                sb.append(entry.getValue()).append("\n");
            }
        } else {
            sb.append("No live ranges computed.\n");
        }

        sb.append("Virtual Nodes:\n");
        for (Node node : vregNodes.values()) {
            sb.append("  ").append(node.vr).append(" -> ");
            sb.append(node.adj.stream().map(n -> n.vr != null ? n.vr.toString() : "").toList()).append("\n");
        }
        sb.append("Physical Nodes:\n");
        for (Node node : physNodes.values()) {
            sb.append("  ").append(node.phys).append(" -> ");
            sb.append(node.adj.stream().map(n -> n.phys != null ? n.phys.toString() : "").toList()).append("\n");
        }
        return sb.toString();
    }

    public Map<VirtualRegister, Integer> getDefs() {
        return defs;
    }

    public void setDefs(Map<VirtualRegister, Integer> defs) {
        this.defs = defs;
    }

    public Map<VirtualRegister, Integer> getUses() {
        return uses;
    }

    public void setUses(Map<VirtualRegister, Integer> uses) {
        this.uses = uses;
    }

    public Map<VirtualRegister, Integer> getLoopWeights() {
        return loopWeights;
    }

    public void setLoopWeights(Map<VirtualRegister, Integer> loopWeights) {
        this.loopWeights = loopWeights;
    }
}
