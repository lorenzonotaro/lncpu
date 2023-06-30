package com.lnasm.compiler;

public interface Encodeable {
    byte[] encode(Linker linker, short addr);
    int size();
}
