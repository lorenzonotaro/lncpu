package com.lnasm.compiler;

public interface Encodeable {
    byte[] encode(Linker linker, Segment currentCs);
    int size();
}
