package com.lnc.cc.codegen;

import com.lnc.cc.ir.IRBlock;

import java.util.*;

/**
 * Control-flow aware live range representation.
 * Stores, for each basic block, a set of disjoint, merged [start,end] instruction index segments.
 */
public final class LiveRange {

    // Per-block live segments (inclusive indices)
    private final Map<IRBlock, List<Segment>> segmentsByBlock = new LinkedHashMap<>();

    // Legacy fields retained for compatibility with callers that might read start()/end()
    // They reflect the min start and max end across all segments.
    private int minStart = Integer.MAX_VALUE;
    private int maxEnd   = Integer.MIN_VALUE;

    public LiveRange() {
    }

    // Backwards compatibility constructor; creates an empty range and sets legacy bounds
    public LiveRange(int start, int end) {
        this.minStart = start;
        this.maxEnd   = end;
    }

    public static final class Segment {
        public int start; // inclusive
        public int end;   // inclusive

        public Segment(int start, int end) {
            if (start > end) {
                int t = start; start = end; end = t;
            }
            this.start = start;
            this.end = end;
        }

        public boolean overlapsOrAdjacent(Segment other) {
            return this.start <= other.end + 1 && other.start <= this.end + 1;
        }

        public void merge(Segment other) {
            this.start = Math.min(this.start, other.start);
            this.end   = Math.max(this.end,   other.end);
        }

        @Override
        public String toString() {
            return "[" + start + "," + end + "]";
        }
    }

    /**
     * Add a single live point (index) for the given block.
     * Points are merged into segments on insertion.
     */
    public void addPoint(IRBlock block, int index) {
        addSpan(block, index, index);
    }

    /**
     * Add or extend a live span in a block, merging with existing segments.
     */
    public void addSpan(IRBlock block, int start, int end) {
        List<Segment> list = segmentsByBlock.computeIfAbsent(block, k -> new ArrayList<>());
        Segment incoming = new Segment(start, end);

        if (list.isEmpty()) {
            list.add(incoming);
        } else {
            // Insert keeping order by start
            int pos = 0;
            while (pos < list.size() && list.get(pos).start <= incoming.start) pos++;
            list.add(pos, incoming);

            // Merge overlapping/adjacent around pos
            // Merge backward
            int i = Math.max(0, pos - 1);
            while (i + 1 < list.size() && list.get(i).overlapsOrAdjacent(list.get(i + 1))) {
                list.get(i).merge(list.get(i + 1));
                list.remove(i + 1);
            }
            // Merge further forward if needed (already handled by while above)
        }

        // Update legacy bounds
        minStart = Math.min(minStart, Math.min(start, end));
        maxEnd   = Math.max(maxEnd,   Math.max(start, end));
    }

    /**
     * Control-flow aware intersection: true if there exists a basic block where
     * the two live ranges overlap in time (their segments overlap).
     */
    public boolean intersects(LiveRange other) {
        // Quick reject by global min/max
        if (this.minStart > this.maxEnd || other.minStart > other.maxEnd) return false;

        // Intersect per-block
        for (Map.Entry<IRBlock, List<Segment>> e : segmentsByBlock.entrySet()) {
            List<Segment> a = e.getValue();
            List<Segment> b = other.segmentsByBlock.get(e.getKey());
            if (b == null || a.isEmpty() || b.isEmpty()) continue;

            int i = 0, j = 0;
            while (i < a.size() && j < b.size()) {
                Segment sa = a.get(i), sb = b.get(j);
                if (sa.end < sb.start) {
                    i++;
                } else if (sb.end < sa.start) {
                    j++;
                } else {
                    // Overlap
                    return true;
                }
            }
        }
        return false;
    }

    public int start() {
        return minStart;
    }

    public int end() {
        return maxEnd;
    }

    public int getSpan() {
        if (segmentsByBlock.isEmpty()) return 0;
        int sum = 0;
        for (List<Segment> list : segmentsByBlock.values()) {
            for (Segment s : list) {
                sum += (s.end - s.start + 1);
            }
        }
        return sum;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        LiveRange that = (LiveRange) obj;
        return Objects.equals(this.segmentsByBlock, that.segmentsByBlock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(segmentsByBlock);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LiveRange{");
        boolean firstB = true;
        for (Map.Entry<IRBlock, List<Segment>> e : segmentsByBlock.entrySet()) {
            if (!firstB) sb.append(", ");
            firstB = false;
            sb.append("B@").append(Integer.toHexString(System.identityHashCode(e.getKey()))).append(":");
            sb.append(e.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}
