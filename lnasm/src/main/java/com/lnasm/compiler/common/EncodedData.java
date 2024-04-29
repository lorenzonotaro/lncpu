package com.lnasm.compiler.common;

import com.lnasm.compiler.linker.AbstractLinker;

public class EncodedData implements IEncodeable {
    private final byte[] data;

    public EncodedData(byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] encode(AbstractLinker linker, short addr) {
        return data;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return data.length;
    }
}
