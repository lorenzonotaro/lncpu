package com.lnc.cc.codegen;

import com.lnc.assembler.common.LabelInfo;
import com.lnc.assembler.parser.CodeElement;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class AsmCursor {
    private final ListIterator<CodeElement> it;
    private CodeElement current;
    private boolean canRemove = false;

    public AsmCursor(LinkedList<CodeElement> list) {
        this.it = list.listIterator();
    }

    /** Advance to the next element, return it. */
    public CodeElement next() {
        current = it.next();
        canRemove = true;
        return current;
    }

    /** True if there’s more instructions. */
    public boolean hasNext() {
        return it.hasNext();
    }

    /** Remove the element most recently returned by next(), moving its labels to the next element if any. */
    public void removeCurrent() {
        if (!canRemove) throw new IllegalStateException();

        // 1) Steal the labels off the current element
        List<LabelInfo> movedLabels = new ArrayList<>(current.getLabels());
        current.clearLabels();

        // 2) Remove the element
        it.remove();
        canRemove = false;

        // 3) If there is a next element, transfer the labels to it
        if (it.hasNext()) {
            CodeElement next = it.next();
            // Prepend or replace? Here we prepend so existing labels remain:
            List<LabelInfo> newLabels = new ArrayList<>(movedLabels);
            newLabels.addAll(next.getLabels());
            next.setLabels(newLabels);
            // Rewind so the cursor remains before that next element
            it.previous();
        }
    }

    /** Insert before the current element (i.e. before the cursor), moving any labels of the current element to the one being added. */
    public void insertBeforeCurrent(CodeElement e) {
        if (current != null) {
            e.setLabels(current.getLabels());
        }
        it.add(e);
        // after add(), cursor is after the inserted element,
        // so we need to rewind back to current:
        it.previous();
        canRemove = false;
    }

    /** Insert a sequence of elements before the current element, in the order they are provided in. */
    public void insertSequenceBeforeCurrent(List<? extends CodeElement> seq) {
        // If we haven't advanced yet, just prepend
        if (current == null) {
            for (CodeElement e : seq) {
                it.add(e);
                it.previous();
            }
            return;
        }

        // Pull the labels off the original instruction
        List<LabelInfo> movedLabels = new ArrayList<>(current.getLabels());
        current.clearLabels();

        // Insert each new element in order
        for (int i = 0; i < seq.size(); i++) {
            CodeElement e = seq.get(i);
            if (i == 0) {
                e.setLabels(movedLabels);
            }
            it.add(e);
            it.previous();
        }

        canRemove = false;
    }

    /** Insert immediately after the current element. */
    public void insertAfterCurrent(CodeElement e) {
        it.add(e);
        // after add(), cursor is after the inserted element,
        // so we need to rewind back to current:
        it.previous();
        canRemove = false;
    }

    /** Insert a sequence after the current element, in the order they are provided in. */
    public void insertSequenceAfterCurrent(List<CodeElement> sequence) {
        for (CodeElement e : sequence) {
            it.add(e);
        }
        // after add(), cursor is after the inserted elements,
        // so we need to rewind back to current:
        it.previous();
        canRemove = false;
    }

    /** Replace the current element with a new one. */
    public void replaceCurrent(CodeElement e) {
        if (!canRemove) throw new IllegalStateException();
        it.set(e);
    }

    public void previous() {
        if (!it.hasPrevious()) {
            throw new IllegalStateException("No previous element to move to.");
        }
        current = it.previous();
        canRemove = true;
    }

    /**
     * Look at the next element without moving the cursor.
     * @return the next CodeElement, or null if at end
     */
    public CodeElement peekNext() {
        if (!it.hasNext()) return null;
        CodeElement next = it.next();
        it.previous();  // rewind so the cursor is unchanged
        return next;
    }
}
