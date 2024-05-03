package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.ILabelSectionLocator;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.linker.AbstractLinker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class Byte extends Argument {
    public final byte value;

    public Byte(Token token) {
        super(token, Type.BYTE);
        this.value = token.ensureByte();
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        return 1;
    }

    @Override
    public void encode(ILabelSectionLocator sectionLocator, AbstractLinker linker, WritableByteChannel channel) throws IOException {
        channel.write(ByteBuffer.wrap(new byte[]{value}));
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        return "cst";
    }
}
