package com.lnc.cc.codegen;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

import java.util.*;

public class InterferenceGraph {
    private final Map<VirtualRegister, Node> vregNodes   = new LinkedHashMap<>();
    private final Map<Register,        Node> physNodes   = new LinkedHashMap<>();

    public static class Node {
        public final VirtualRegister vr;
        public final Register        phys;
        public final Set<Node>       adj = new LinkedHashSet<>();

        public Register    precolored = null;
        public Register              assigned;

        private Node(VirtualRegister vr) {
            this.vr   = vr; this.phys = null;
            if (vr.getRegisterClass().isSingleton())
                this.precolored = vr.getRegisterClass().onlyRegister();
        }

        private Node(Register phys) {
            this.vr = null;
            this.phys = phys;
            this.precolored = phys;
            this.assigned   = phys;
        }

        public boolean isPhysical() { return phys != null; }

        public Set<Register> allowedColors() {
            return isPhysical()
                    ? Set.of(phys)
                    : vr.getRegisterClass().getRegisters();
        }
    }

    public InterferenceGraph() {
        // create a node for every phys‐reg
        for (Register r : Register.values()) {
            physNodes.put(r, new Node(r));
        }
        // now link each compound‐phys to its components
        for (Register r : Register.values()) {
            if (r.isCompound()) {
                Node compNode = physNodes.get(r);
                for (Register sub : r.getComponents()) {
                    Node subNode = physNodes.get(sub);
                    // mutual interference so they never share
                    compNode.adj.add(subNode);
                    subNode.adj.add(compNode);
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

    public static Map<VirtualRegister, LiveRange> computeLiveRanges(IRUnit unit, LivenessInfo livenessInfo) {
        Map<VirtualRegister, LiveRange> liveRanges = new HashMap<>();

        for (VirtualRegister vr : unit.getVirtualRegisterManager().getAllRegisters()) {
            liveRanges.put(vr, new LiveRange());
        }

        var rpo = unit.computeReversePostOrderAndCFG();

        int index = 0;
        // walk the RPO to set the indices
        for (IRBlock block : rpo) {
            for (IRInstruction inst = block.getFirst(); inst != null; inst = inst.getNext()) {
                inst.setIndex(index++);
            }
        }

        Map<IRBlock,Set<VirtualRegister>> liveIn = livenessInfo.liveIn(), liveOut = livenessInfo.liveOut();

        // 2) number instructions in RPO as you already do…

        // 3) now collect intervals
        for (IRBlock bb : rpo) {
            // start this block’s scan with the regs live on exit:
            Set<VirtualRegister> live = new HashSet<>(liveOut.get(bb));

            // scan instructions *backwards*, updating start/end exactly as before:
            for (IRInstruction inst = bb.getLast(); inst != null; inst = inst.getPrev()) {
                int i = inst.getIndex();
                // kill
                for (IROperand def : inst.getWrites()) {
                    if(def instanceof VirtualRegister vr) {
                        live.remove(def);
                    }
                }
                // gen
                for (IROperand use : inst.getReads()) {
                    if(use instanceof VirtualRegister vr) {
                        live.add(vr);
                        LiveRange lr = liveRanges.get(vr);
                        lr.start = Math.min(lr.start, i);
                        lr.end = Math.max(lr.end, i);
                    }
                }
                // extend
                for (VirtualRegister v : live) {
                    LiveRange lr = liveRanges.get(v);
                    lr.end = Math.max(lr.end, i);
                }
            }
        }

        return liveRanges;
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

        // Pre-color fixed nodes
        for (VirtualRegister vr : unit.getVirtualRegisterManager().getAllRegisters()) {
            if (vr.getRegisterClass().isSingleton()) {
                graph.getNode(vr).precolored = vr.getRegisterClass().onlyRegister();
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
                    VirtualRegister rhs  = (VirtualRegister) bin.getRight();
                    VirtualRegister lhs  = (VirtualRegister) bin.getLeft();
                    graph.addEdge(dest, rhs);
                    graph.addEdge(dest, lhs);
                }
            }
        }

        return graph;
    }

}
