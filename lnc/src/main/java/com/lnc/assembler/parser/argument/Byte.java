package com.lnc.assembler.parser.argument;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.linker.LinkInfo;

import java.io.IOException;

public class Byte extends NumericalArgument {
    public final byte value;

    public Byte(Token token) {
        super(token, Type.BYTE);
        this.value = token.ensureByte();
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return 1;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        return new byte[] { value };
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return "cst";
    }

    @Override
    public int value(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        return value & 0xFF;
    }
}
