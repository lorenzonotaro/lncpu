package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.*;
import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class Word extends Argument {
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
    public void encode(ILabelResolver labelResolver, WritableByteChannel channel, int instructionAddress) throws IOException {
        channel.write(ByteBuffer.wrap(
                new byte[]{
                        (byte) (value & 0xFF),
                        (byte) ((value >> 8) & 0xFF)
                }
        ));
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return "dcst";
    }
}
