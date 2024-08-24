package com.lnasm.compiler.parser;

import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;
import com.lnasm.compiler.linker.LinkInfo;

import java.io.IOException;

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
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) throws IOException {
        return data;
    }

    public static EncodedData of(byte[] data){
        return new EncodedData(data);
    }


}
