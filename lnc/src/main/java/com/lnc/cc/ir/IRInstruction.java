package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents an abstract IR (Intermediate Representation) instruction used in compiler backends
 * or intermediate representations of programming languages. This class provides support for
 * linked-list style instruction chaining, read/write operand tracking, and manipulation within
 * a block of instructions.
 */
public abstract class IRInstruction {

    private static int UNIQUE_ID_COUNTER = 0;

    private final int uniqueId;

    private int index;

    private int loopNestedLevel = 0;

    protected IRInstruction prev;

    protected IRInstruction next;
    private IRBlock parentBlock;

    public abstract <E> E accept(IIRInstructionVisitor<E> visitor);

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public IRInstruction getPrev() {
        return prev;
    }

    public void setPrev(IRInstruction prev) {
        this.prev = prev;
    }

    public IRInstruction getNext() {
        return next;
    }

    public void setNext(IRInstruction next) {
        this.next = next;
    }

    public IRInstruction() {
        this.uniqueId = UNIQUE_ID_COUNTER++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IRInstruction that = (IRInstruction) o;
        return uniqueId == that.uniqueId;
    }

    @Override
    public int hashCode() {
        return uniqueId;
    }

    public boolean hasNext() {
        return next != null;
    }

    public boolean hasPrev() {
        return prev != null;
    }

    public abstract String toString();

    void replaceWith(IRInstruction other) {
        other.prev = this.prev;
        other.next = this.next;
        if (this.prev != null) this.prev.next = other;
        if (this.next != null) this.next.prev = other;
        if (parentBlock != null) {
            if (parentBlock.first == this) parentBlock.first = other;
            if (parentBlock.last == this) parentBlock.last = other;
            other.parentBlock = parentBlock;
        }
    }

    public void remove() {
        if (prev != null) prev.next = next;
        if (next != null) next.prev = prev;
        if (parentBlock != null) {
            if (parentBlock.first == this) parentBlock.first = next;
            if (parentBlock.last == this) parentBlock.last = prev;
        }
        prev = next = null;
        parentBlock = null;
    }

    public void insertBefore(IRInstruction other) {
        other.prev = this.prev;
        other.next = this;
        if (this.prev != null)
            this.prev.next = other;

        this.prev = other;

        other.parentBlock = this.parentBlock;

        if (this.parentBlock != null && this.parentBlock.first == this) {
            this.parentBlock.first = other;
        }
    }

    public void insertAfter(IRInstruction other) {
        other.next = this.next;
        other.prev = this;
        if (this.next != null) this.next.prev = other;
        this.next = other;
        other.parentBlock = this.parentBlock;

        if (this.parentBlock != null && this.parentBlock.last == this) {
            this.parentBlock.last = other;
        }
    }

    public void insertBefore(List<IRInstruction> sequence) {
        if (sequence.isEmpty()) return;

        // Link the sequence
        linkSequence(sequence);

        IRInstruction first = sequence.get(0);
        IRInstruction last = sequence.get(sequence.size() - 1);

        first.prev = this.prev;
        last.next = this;
        if (this.prev != null) this.prev.next = first;
        this.prev = last;

        for (IRInstruction instr : sequence) {
            instr.parentBlock = this.parentBlock;
        }

        if (this.parentBlock != null && this.parentBlock.first == this) {
            this.parentBlock.first = first;
        }
    }

    public void insertAfter(List<IRInstruction> sequence) {
        if (sequence.isEmpty()) return;

        linkSequence(sequence);

        IRInstruction first = sequence.get(0);
        IRInstruction last = sequence.get(sequence.size() - 1);

        last.next = this.next;
        first.prev = this;
        if (this.next != null) this.next.prev = last;
        this.next = first;

        for (IRInstruction instr : sequence) {
            instr.parentBlock = this.parentBlock;
        }

        if (this.parentBlock != null && this.parentBlock.last == this) {
            this.parentBlock.last = last;
        }
    }

    private static void linkSequence(List<IRInstruction> sequence) {
        // Link the sequence
        for (int i = 0; i < sequence.size() - 1; i++) {
            sequence.get(i).next = sequence.get(i + 1);
            sequence.get(i + 1).prev = sequence.get(i);
        }
    }

    public void setParentBlock(IRBlock block) {
        this.parentBlock  = block;
    }

    public IRBlock getParentBlock() {
        if (parentBlock == null) {
            throw new IllegalStateException("Instruction is not associated with any block.");
        }
        return parentBlock;
    }

    public final Collection<VirtualRegister> getReads(){
        return Stream.concat(getReadOperands().stream()
                .flatMap((IROperand irOperand) -> irOperand instanceof VirtualRegister vr ? Stream.of(vr) : irOperand.getVRReads().stream()),
                getWriteOperands().stream()
                        .flatMap((IROperand irOperand) -> irOperand instanceof VirtualRegister ? Stream.empty() : irOperand.getVRReads().stream())
        ).toList();
    }

    public final Collection<VirtualRegister> getWrites(){
        return getWriteOperands().stream()
                .flatMap((IROperand irOperand) -> irOperand instanceof VirtualRegister vr ? Stream.of(vr) : irOperand.getVRWrites().stream())
                .toList();
    }

    public abstract Collection<IROperand> getReadOperands();

    protected abstract Collection<IROperand> getWriteOperands();

    public abstract void replaceOperand(IROperand oldOp, IROperand newOp);
}
