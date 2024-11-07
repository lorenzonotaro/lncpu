package com.lnc.cc.ir;

public record LoopInfo(IRBlock continueTarget, IRBlock breakTarget) {
    public LoopInfo {
        if (continueTarget == null) {
            throw new IllegalArgumentException("continueTarget cannot be null");
        }
        if (breakTarget == null) {
            throw new IllegalArgumentException("breakTarget cannot be null");
        }
    }
}
