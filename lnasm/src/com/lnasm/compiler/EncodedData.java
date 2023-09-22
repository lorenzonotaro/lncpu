package com.lnasm.compiler;

public class EncodedData implements Encodeable{
    private final byte[] data;

    public EncodedData(byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] encode(AbstractLinker linker, short addr) {
        return data;
    }

    @Override
    public int size() {
        return data.length;
    }
}
