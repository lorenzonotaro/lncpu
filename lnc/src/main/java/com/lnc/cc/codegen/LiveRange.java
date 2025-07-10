package com.lnc.cc.codegen;

import java.util.Objects;

public final class LiveRange {
    int start;
    int end;

    public LiveRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public LiveRange() {
        this(Integer.MAX_VALUE, Integer.MIN_VALUE);
    }

    public boolean intersects(LiveRange other) {
        return start < other.end && end > other.start;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LiveRange) obj;
        return this.start == that.start &&
                this.end == that.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "LiveRange[" +
                "start=" + start + ", " +
                "end=" + end + ']';
    }

    public int getSpan() {
        return end - start + 1;
    }
}
