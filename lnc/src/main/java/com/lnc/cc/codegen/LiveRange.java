package com.lnc.cc.codegen;

public record LiveRange(int start, int end) {
    public boolean intersects(LiveRange other) {
        return start <= other.end && end >= other.start;
    }
}
