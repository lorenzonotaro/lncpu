package com.lnc.assembler.parser.argument;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.linker.LinkInfo;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

import java.io.IOException;

public class Word extends NumericalArgument {
    public final short value;

    public Word(Token token) {
        super(token, Type.WORD);
        this.value = token.ensureShort();
    }

    public Word(Token token, int value) {
        super(token, Type.WORD);
        if (IntUtils.inShortRange(value)) {
            this.value = (short) (value & 0xFFFF);
        } else {
            throw new CompileException("value out of range for word", token);
        }
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return 2;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        return new byte[]{
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    @Override
    public String toString() {
        return String.format("0x%04x", value & 0xFFFF);
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return "dcst";
    }

    @Override
    public int value(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        return value;
    }
}
