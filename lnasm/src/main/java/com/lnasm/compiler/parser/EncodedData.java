package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.ILabelSectionLocator;
import com.lnasm.compiler.linker.AbstractLinker;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class EncodedData extends CodeElement{

    private final byte[] data;

    private EncodedData(byte[] data) {
        this.data = data;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        return data.length;
    }

    @Override
    public void encode(ILabelSectionLocator sectionLocator, AbstractLinker linker, WritableByteChannel channel) throws IOException {
        channel.write(java.nio.ByteBuffer.wrap(data));
    }

    public static EncodedData of(byte[] data){
        return new EncodedData(data);
    }


}
