package com.lnc.assembler.parser;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.linker.LinkInfo;
import com.lnc.cc.codegen.CodeElementVisitor;

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
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        return data;
    }

    public static EncodedData of(byte[] data){
        return new EncodedData(data);
    }


    @Override
    public <T> T accept(CodeElementVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
