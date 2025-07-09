package com.lnc.cc.ir;


import com.lnc.assembler.parser.Instruction;

import java.util.*;

public class IRBlock implements Iterable<IRInstruction> {

    private final IRUnit unit;

    private final int id;

    private final List<IRBlock> predecessors = new ArrayList<>();

    private List<IRBlock> successors = new ArrayList<>();

    protected IRInstruction first = null;

    protected IRInstruction last = null;

    public IRBlock(IRUnit unit, int id) {
        this.unit = unit;
        this.id = id;
    }

    public void emitFirst(IRInstruction instruction) {

        instruction.setParentBlock(this);

        instruction.next = first;
        instruction.prev = null;
        this.first = instruction;
    }

    public void emit(IRInstruction instruction) {
        if (last instanceof AbstractBranchInstr abi) {

            if (abi instanceof Goto) {
                throw new IllegalStateException("Cannot emit instruction after a Goto instruction: " + last);
            }

            if (!(instruction instanceof AbstractBranchInstr)) {
                throw new IllegalStateException("Cannot emit non-branch instruction after a branch instruction: " + last);
            }
        }

        instruction.setParentBlock(this);

        if (first == null) {
            first = instruction;
        } else {
            last.setNext(instruction);
            instruction.setPrev(last); // Maintain the two-way link
        }

        last = instruction;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "_l" + id;
    }

    public List<IRBlock> getPredecessors() {
        return predecessors;
    }

    private boolean isEmpty() {
        return first == null;
    }

    public void updateReferences(IRBlock oldBlock, IRBlock newBlock){
        for (IRInstruction instruction : this) {
            if (instruction instanceof AbstractBranchInstr branch) {
                branch.replaceReference(oldBlock, newBlock);
            }
        }
    }


    public void replaceWith(IRBlock newBlock) {

        if (newBlock == null) {
            throw new IllegalArgumentException("New block cannot be null.");
        }

        if (newBlock == this) {
            throw new IllegalArgumentException("New block cannot be the same as the current block.");
        }

        for (IRBlock successor : successors) {
            successor.predecessors.remove(this);
            if(newBlock != successor) {
                successor.predecessors.add(newBlock);
                newBlock.successors.add(successor);
            }
        }

        for (IRBlock predecessor : predecessors) {
            predecessor.updateReferences(this, newBlock);
            predecessor.successors.remove(this);
            if(newBlock != predecessor) {
                predecessor.successors.add(newBlock);
                newBlock.predecessors.add(predecessor);
            }
        }

        // Special case for when this block is the entry block of the unit
        if (unit.getEntryBlock() == this) {
            unit.setEntryBlock(newBlock);
        }
    }

    @Override
    public Iterator<IRInstruction> iterator() {
        return new Iterator<>() {
            private IRInstruction current = first;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public IRInstruction next() {
                if (current == null) {
                    throw new NoSuchElementException("No more instructions in block " + id);
                }
                IRInstruction instr = current;
                current = current.getNext();
                return instr;
            }
        };
    }

    public List<IRBlock> getSuccessors() {
        return successors;
    }

    public IRInstruction getFirst() {
        return first;
    }

    public IRInstruction getLast() {
        return last;
    }

    public void setLast(IRInstruction last) {
        if (last == null) {
            throw new IllegalArgumentException("Last instruction cannot be null.");
        }
        this.last = last;
        last.setParentBlock(this);
    }

    public Collection<IRBlock> getLastInstructionTargets() {
        if (last instanceof AbstractBranchInstr branch) {
            return branch.getTargets();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IRBlock irBlock)) return false;
        return id == irBlock.id && Objects.equals(unit, irBlock.unit);
    }
}
