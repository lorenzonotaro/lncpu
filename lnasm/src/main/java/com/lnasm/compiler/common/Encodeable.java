package com.lnasm.compiler.common;

import com.lnasm.compiler.linker.AbstractLinker;

public interface Encodeable {
    byte[] encode(AbstractLinker linker, short addr);
    int size();
}
