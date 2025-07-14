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
            ranges.put(vr, new LiveRange(Integer.MAX_VALUE, Integer.MIN_VALUE));

        // 2) number instructions in forward RPO
        List<IRBlock> rpo = unit.computeReversePostOrderAndCFG();
        int idx = 0;
        for (IRBlock B : rpo) {
            for (IRInstruction inst = B.getFirst(); inst != null; inst = inst.getNext()) {
                inst.setIndex(idx++);
            }
        }

        // 3) backward scan per block, seeded with liveOut
        for (IRBlock B : rpo) {
            var blockLoopWeight = B.getLoopDepth();
            Set<VirtualRegister> live = new HashSet<>(li.liveOut().get(B));
            for (IRInstruction inst = B.getLast(); inst != null; inst = inst.getPrev()) {
                int i = inst.getIndex();

                // kill defs *and* record def position
                for (VirtualRegister d : inst.getWrites()) {
                    live.remove(d);
                    LiveRange lr = ranges.get(d);
                    lr.start = Math.min(lr.start, i);
                    lr.end   = Math.max(lr.end,   i);
                    defs.put(d, defs.getOrDefault(d, 0) + 1);

                    loopWeights.put(d, Math.max(loopWeights.getOrDefault(d, 0), blockLoopWeight * 10));
                }

                // gen uses *and* record use position
                for (VirtualRegister u : inst.getReads()) {
                    live.add(u);
                    LiveRange lr = ranges.get(u);
                    lr.start = Math.min(lr.start, i);
                    lr.end   = Math.max(lr.end,   i);

                    uses.put(u, uses.getOrDefault(u, 0) + 1);
                    loopWeights.put(u, Math.max(loopWeights.getOrDefault(u, 0), blockLoopWeight * 10));
                }

                // extend all still‐live
                for (VirtualRegister v : live) {
                    LiveRange lr = ranges.get(v);
                    lr.end = Math.max(lr.end, i);
                }
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

        // 1) compute live ranges
        VrInfo vrInfo = computeVrInfo(unit, livenessInfo);

        Map<VirtualRegister, LiveRange> liveRanges = vrInfo.liveRanges();
        Map<VirtualRegister, Integer> loopWeights = vrInfo.loopWeights();
        Map<VirtualRegister, Integer> uses = vrInfo.uses();
        Map<VirtualRegister, Integer> defs = vrInfo.defs();

        graph.setLiveRanges(liveRanges);
        graph.setLoopWeights(loopWeights);
        graph.setUses(uses);
        graph.setDefs(defs);

        var liveOut = livenessInfo.liveOut();
        var liveIn = livenessInfo.liveIn();


        // 2) add edges for each pair of live ranges that overlap
        for(var a : liveRanges.keySet()){
            for(var b : liveRanges.keySet()){
                if(a == b) continue; // skip self-loops
                LiveRange la = liveRanges.get(a), lb = liveRanges.get(b);
                if(la.intersects(lb)) {
                    graph.addEdge(a, b); // add interference edge
                }
            }
        }

        for (IRBlock B : unit.computeReversePostOrderAndCFG()) {
            for (IRInstruction inst = B.getFirst(); inst != null; inst = inst.getNext()) {

                // Call clobber registers
                if (inst instanceof Call call) {
                    // which phys regs get clobbered?
                    var ret = call.getReturnTarget();

                    if(ret != null){
                        // anyone live across the call cannot take those colors
                        for (VirtualRegister vr : liveOut.get(B)) {
                            for (Register phys : ret.getRegisterClass().getRegisters()) {
                                graph.addEdge(vr, phys);
                            }
                        }
                    }
                }else if(inst instanceof Bin bin){
                    VirtualRegister dest = (VirtualRegister) bin.getDest();
                    VirtualRegister rhs = bin.getRight().type == IROperand.Type.VIRTUAL_REGISTER ? (VirtualRegister) bin.getRight() : null;
                    VirtualRegister lhs = bin.getLeft().type == IROperand.Type.VIRTUAL_REGISTER ? (VirtualRegister) bin.getLeft() : null;
                    if (lhs != null)
                        graph.addPreference(dest, lhs);   // dotted line, not interference
                    if (rhs != null) {
                        if(bin.getOperator().isCommutative()){
                            graph.addPreference(dest, rhs);
                        }else{
                            graph.addEdge(dest, rhs); // interference edge
                        }
                    }
                }else if (inst instanceof Move mv) {
                    IROperand s = mv.getSource(), d = mv.getDest();
                    if (s instanceof VirtualRegister vs && d instanceof VirtualRegister vd)
                        graph.addPreference(vs, vd);
                }else if(inst instanceof Unary un){
                    VirtualRegister dest = (VirtualRegister) un.getTarget();
                    VirtualRegister src = un.getOperand().type == IROperand.Type.VIRTUAL_REGISTER ? (VirtualRegister) un.getOperand() : null;
                    if (src != null)
                        graph.addPreference(dest, src);   // dotted line, not interference
                }
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
