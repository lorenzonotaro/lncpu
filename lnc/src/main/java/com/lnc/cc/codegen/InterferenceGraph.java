package com.lnc.cc.codegen;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;
import com.lnc.cc.types.FunctionType;

import java.util.*;

public class InterferenceGraph {
    private final Map<VirtualRegister, Node> vregNodes   = new LinkedHashMap<>();
    private final Map<Register,        Node> physNodes   = new LinkedHashMap<>();

    // ✨ NEW: copy‑preference (move) edges. Stored as Node pairs.
    private final Set<AbstractMap.SimpleEntry<Node,Node>> moveEdges  = new LinkedHashSet<>();

    private Map<VirtualRegister, LiveRange> liveRanges;

    public static class Node {
        public final VirtualRegister vr;
        public final Register        phys;
        public final Set<Node>       adj = new LinkedHashSet<>();

        public Register assigned   = null;

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
        public Set<Register> allowedColors() {
            return isPhysical()
                    ? new LinkedHashSet<>(Set.of(phys))
                    : vr.getRegisterClass().getRegisters();
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

    public static Map<VirtualRegister, LiveRange> computeLiveRanges(
            IRUnit unit,
            LivenessInfo li
    ) {
        // 1) create empty ranges
        Map<VirtualRegister,LiveRange> ranges = new LinkedHashMap<>();
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
            Set<VirtualRegister> live = new HashSet<>(li.liveOut().get(B));
            for (IRInstruction inst = B.getLast(); inst != null; inst = inst.getPrev()) {
                int i = inst.getIndex();

                // kill defs *and* record def position
                for (IROperand w : inst.getWrites()) {
                    if (w instanceof VirtualRegister d) {
                        live.remove(d);
                        LiveRange lr = ranges.get(d);
                        lr.start = Math.min(lr.start, i);
                        lr.end   = Math.max(lr.end,   i);
                    }
                }

                // gen uses *and* record use position
                for (IROperand r : inst.getReads()) {
                    if (r instanceof VirtualRegister u) {
                        live.add(u);
                        LiveRange lr = ranges.get(u);
                        lr.start = Math.min(lr.start, i);
                        lr.end   = Math.max(lr.end,   i);
                    }
                }

                // extend all still‐live
                for (VirtualRegister v : live) {
                    LiveRange lr = ranges.get(v);
                    lr.end = Math.max(lr.end, i);
                }
            }
        }

        return ranges;
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
        Map<VirtualRegister, LiveRange> liveRanges = computeLiveRanges(unit, livenessInfo);

        graph.setLiveRanges(liveRanges);

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

        // Call clobber registers
        for (IRBlock B : unit.computeReversePostOrderAndCFG()) {
            for (IRInstruction inst = B.getFirst(); inst != null; inst = inst.getNext()) {
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
                    VirtualRegister dest = (VirtualRegister) bin.getTarget();
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

}
