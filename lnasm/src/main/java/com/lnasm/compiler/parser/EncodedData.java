package com.lnasm.compiler.parser;

import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class EncodedData extends CodeElement{

    private final byte[] data;

    private EncodedData(byte[] data) {
        this.data = data;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return data.length;
    }

    @Override
    public void encode(ILabelResolver labelResolver, WritableByteChannel channel, int instructionAddress) throws IOException {
        channel.write(java.nio.ByteBuffer.wrap(data));
    }

    public static EncodedData of(byte[] data){
        return new EncodedData(data);
    }


}
