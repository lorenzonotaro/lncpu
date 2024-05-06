package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.parser.RegisterId;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

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
    public void encode(ILabelResolver labelResolver, WritableByteChannel channel, int instructionAddress) throws IOException {

    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return reg.toString();
    }
}
