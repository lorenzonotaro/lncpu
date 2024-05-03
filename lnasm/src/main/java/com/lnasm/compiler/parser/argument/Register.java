package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.ILabelSectionLocator;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.linker.AbstractLinker;
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
    public int size(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        return 0;
    }

    @Override
    public void encode(ILabelSectionLocator sectionLocator, AbstractLinker linker, WritableByteChannel channel) throws IOException {

    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        return reg.toString();
    }
}
