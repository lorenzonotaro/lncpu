package com.lnc.cc.ir;

import java.util.List;
import java.util.Objects;

public abstract class IRInstruction {

    private int index;

    private int loopNestedLevel = 0;

    private IRInstruction prev;
    private IRInstruction next;

    public abstract <E> E accept(IIRVisitor<E> visitor);

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IRInstruction that = (IRInstruction) o;
        return index == that.index && Objects.equals(prev, that.prev) && Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        return index;
    }

    public int getLoopNestedLevel() {
        return loopNestedLevel;
    }

    public void setLoopNestedLevel(int loopNestedLevel) {
        this.loopNestedLevel = loopNestedLevel;
    }

    public void onRemove(){
    }

    public boolean hasNext() {
        return next != null;
    }

    public boolean hasPrev() {
        return prev != null;
    }
}
