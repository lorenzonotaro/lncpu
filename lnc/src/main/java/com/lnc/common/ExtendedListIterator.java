package com.lnc.common;

import java.util.*;

/**
 * This class implements the ListIterator interface and provides additional functionalities
 * to interact with a LinkedList. It wraps around a standard ListIterator and exposes custom
 * operations such as inserting or removing elements relative to the current position in the list.
 *
 * @param <T> The type of elements in the list being iterated over.
 */
public class ExtendedListIterator<T> implements ListIterator<T> {
    private final LinkedList<T> list;
    private final ListIterator<T> delegate;
    private int lastReturnedIndex = -1;

    public ExtendedListIterator(LinkedList<T> list) {
        this.list = list;
        this.delegate = list.listIterator();
    }

    public ExtendedListIterator(LinkedList<T> list, int index) {
        this.list = list;
        this.delegate = list.listIterator(index);
    }

    private void checkLastReturned() {
        if (lastReturnedIndex < 0) {
            throw new IllegalStateException("No current element (must call next() or previous() first)");
        }
    }

    private void moveToIndex(int targetIndex) {
        while (delegate.nextIndex() < targetIndex) {
            delegate.next();
        }
        while (delegate.nextIndex() > targetIndex) {
            delegate.previous();
        }
    }

    public void addBeforeCurrent(T e) {
        checkLastReturned();
        int currIdx = lastReturnedIndex;
        moveToIndex(currIdx);
        delegate.add(e);
        // inserted at currIdx, original and subsequent shift right by 1
        lastReturnedIndex = currIdx + 1;
        // restore iterator position to after inserted at same distance
        moveToIndex(lastReturnedIndex + (delegate.nextIndex() - currIdx));
    }

    public void addSequenceBeforeCurrent(Collection<? extends T> seq) {
        checkLastReturned();
        int currIdx = lastReturnedIndex;
        moveToIndex(currIdx);
        int count = 0;
        for (T e : seq) {
            delegate.add(e);
            count++;
        }
        // shift current index
        lastReturnedIndex = currIdx + count;
        moveToIndex(lastReturnedIndex + 1);
    }

    public void addAfterCurrent(T e) {
        checkLastReturned();
        int currIdx = lastReturnedIndex;
        moveToIndex(currIdx + 1);
        delegate.add(e);
        // insertion after current does not affect current index
        // but elements between cursor and current? Iterator at after insertion
        moveToIndex(currIdx + 2);
    }

    public void addSequenceAfterCurrent(Collection<? extends T> seq) {
        checkLastReturned();
        int currIdx = lastReturnedIndex;
        moveToIndex(currIdx + 1);
        int count = 0;
        for (T e : seq) {
            delegate.add(e);
            count++;
        }
        // current remains at currIdx
        moveToIndex(currIdx + count + 1);
    }

    public T peek() {
        int idx = delegate.nextIndex();
        if (idx < list.size()) {
            return list.get(idx);
        }
        throw new NoSuchElementException();
    }

    public void removeCurrent() {
        checkLastReturned();
        int currIdx = lastReturnedIndex;
        moveToIndex(currIdx + 1);
        delegate.previous();
        delegate.remove();
        lastReturnedIndex = -1;
        moveToIndex(currIdx);
    }

    public void removeNext() {
        int nxt = delegate.nextIndex();
        if (nxt >= list.size()) {
            throw new NoSuchElementException();
        }
        moveToIndex(nxt + 1);
        delegate.previous();
        delegate.remove();
        // current unaffected
        moveToIndex(nxt);
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public T next() {
        T e = delegate.next();
        lastReturnedIndex = delegate.previousIndex();
        return e;
    }

    @Override
    public boolean hasPrevious() {
        return delegate.hasPrevious();
    }

    @Override
    public T previous() {
        T e = delegate.previous();
        lastReturnedIndex = delegate.nextIndex();
        return e;
    }

    @Override
    public int nextIndex() {
        return delegate.nextIndex();
    }

    @Override
    public int previousIndex() {
        return delegate.previousIndex();
    }

    @Override
    public void remove() {
        delegate.remove();
        lastReturnedIndex = -1;
    }

    @Override
    public void set(T e) {
        delegate.set(e);
    }

    @Override
    public void add(T e) {
        delegate.add(e);
        lastReturnedIndex = -1;
    }
}
