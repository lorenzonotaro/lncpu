package com.lnc.cc.ir;

import java.util.*;

public class VirtualRegisterManager {

    private final Node head;
    private Node last;

    private final Map<Integer, Node> registerMap;

    private final PriorityQueue<Node> nextAvailable;

    VirtualRegisterManager(){
        this.registerMap = new HashMap<>();
        this.nextAvailable = new PriorityQueue<>(Comparator.comparingInt(o -> o.id));
        this.head = this.last = createNode();
    }

    private Node createNode() {
        Node node;

        if(last == null){
            node = new Node(0, null);
        }else{
            node = new Node(last.id + 1, null);
            last.next = node;
        }

        registerMap.put(node.id, node);

        last = node;

        return node;
    }

    VirtualRegister getRegister(){
        if(!nextAvailable.isEmpty()){
            Node node = nextAvailable.poll();
            return node.reInstance();
        }else{
            Node node = createNode();
            last.next = node;
            last = node;
            return node.register;
        }
    }


    void releaseRegister(VirtualRegister register){

        Node node = registerMap.get(register.getRegisterNumber());

        if(node == null){
            throw new IllegalArgumentException("Register not managed by this manager");
        }

        if(node.register == null || !node.register.equals(register)){
            throw new IllegalArgumentException("Register already released");
        }

        node.register.release();

        node.register = null;

        nextAvailable.add(node);
    }



    private static class Node{
        private VirtualRegister register;
        private final int id;
        private Node next;

        private Node(int id, Node next){
            this.id = id;
            this.register = new VirtualRegister(id);
            this.next = next;
        }

        private VirtualRegister reInstance(){
            return register = new VirtualRegister(id);
        }

        @Override
        public int hashCode() {
            return register.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Node node = (Node) o;
            return id == node.id && Objects.equals(register, node.register) && Objects.equals(next, node.next);
        }
    }
}
