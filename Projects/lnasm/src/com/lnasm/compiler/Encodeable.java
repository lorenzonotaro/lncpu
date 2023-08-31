package com.lnasm.compiler;

public interface Encodeable {
    byte[] encode(AbstractLinker linker, short addr);
    int size();
}
