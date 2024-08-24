package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.linker.LinkInfo;

import java.io.IOException;

public class Byte extends Argument {
    public final byte value;

    public Byte(Token token) {
        super(token, Type.BYTE, true);
        this.value = token.ensureByte();
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return 1;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) throws IOException {
        return new byte[] { value };
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return "cst";
    }
}
