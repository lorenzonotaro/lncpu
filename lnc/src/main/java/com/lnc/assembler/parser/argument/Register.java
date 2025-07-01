package com.lnc.assembler.parser.argument;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.linker.LinkInfo;
import com.lnc.assembler.parser.RegisterId;

import java.io.IOException;

public class Register extends Argument {
    public final RegisterId reg;

    public Register(Token token) {
        super(token, Type.REGISTER);
        this.reg = RegisterId.fromString(token.lexeme);
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return 0;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        return new byte[0];
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return reg.toString();
    }
}
