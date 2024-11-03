package com.lnc.cc.codegen;

import com.lnc.cc.ir.VirtualRegister;
import com.lnc.cc.optimization.LinearIRUnit;

import java.util.*;
import java.util.stream.Collectors;

public class GraphColoringRegisterAllocator {
    private LinearIRUnit linearIRUnit;

    private List<InterferenceGraphNode> interferenceGraphNodes;

    public Set<Register> usedRegisters = new HashSet<>();

    public GraphColoringRegisterAllocator(LinearIRUnit linearIRUnit) {
        this.linearIRUnit = linearIRUnit;
        this.interferenceGraphNodes = new ArrayList<>();

        buildInterferenceGraph();
    }

    private void buildInterferenceGraph() {
        var registers = linearIRUnit.nonLinearUnit.getVRManager().getAllRegisters();

        for (VirtualRegister register : registers) {
            interferenceGraphNodes.add(new InterferenceGraphNode(register));
        }

        for (InterferenceGraphNode node : interferenceGraphNodes) {
            for (InterferenceGraphNode other : interferenceGraphNodes) {
                if (!node.equals(other) && node.liveRange.intersects(other.getRegister().getLiveRange())) {
                    node.addNeighbor(other);
                    other.addNeighbor(node);
                }
            }
        }
    }

    public void allocate() {
        var priorityOrder = interferenceGraphNodes.stream().sorted(Comparator.comparingInt(InterferenceGraphNode::getDegree).reversed().thenComparing(e -> e.getRegister().getRegisterClass().getSize()).thenComparing(InterferenceGraphNode::getSpillCost)).toList();

        for (var node : priorityOrder){
            var neighborAssignments = node.getNeighbors().stream().map(e -> e.getRegister().getAssignedPhysicalRegister()).filter(Objects::nonNull).collect(Collectors.toSet());

            var nextInClass = node.getRegister().getRegisterClass().next(neighborAssignments);

            if(nextInClass != null){
                node.getRegister().setAssignedPhysicalRegister(nextInClass);
                usedRegisters.add(nextInClass);
            }else{
                System.out.println("Spilling register " + node.getRegister());
                // ONLY FOR DEBUG PURPOSES
                node.getRegister().setAssignedPhysicalRegister(Register.RX);
            }
        }
    }

    private class InterferenceGraphNode {
        private final VirtualRegister register;
        private final List<InterferenceGraphNode> neighbors;
        private final LiveRange liveRange;

        public InterferenceGraphNode(VirtualRegister register) {
            this.register = register;
            this.liveRange = register.getLiveRange();
            neighbors = new ArrayList<>();
        }

        public VirtualRegister getRegister() {
            return register;
        }

        public int getSpillCost() {
          return register.spillCost();
        }

        public void addNeighbor(InterferenceGraphNode neighbor) {
            neighbors.add(neighbor);
        }

        public List<InterferenceGraphNode> getNeighbors() {
            return neighbors;
        }

        public boolean isNeighbor(InterferenceGraphNode node) {
            return neighbors.contains(node);
        }

        public int getDegree() {
            return neighbors.size();
        }

        public void removeNeighbor(InterferenceGraphNode node) {
            neighbors.remove(node);
        }

        public void removeAllNeighbors() {

            for (InterferenceGraphNode neighbor : neighbors) {
                neighbor.removeNeighbor(this);
            }

            neighbors.clear();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InterferenceGraphNode that = (InterferenceGraphNode) o;
            return register.equals(that.register) && neighbors.equals(that.neighbors);
        }

        @Override
        public int hashCode() {
            int result = register.hashCode();
            result = 31 * result + neighbors.hashCode();
            return result;
        }
    }

}
