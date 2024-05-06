package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;
import com.lnasm.compiler.common.Token;

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
    public int size(ILabelSectionLocator sectionLocator) {
        return 1;
    }

    @Override
    public void encode(ILabelResolver labelResolver, WritableByteChannel channel, int instructionAddress) throws IOException {
        channel.write(ByteBuffer.wrap(new byte[]{value}));
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return "cst";
    }
}
