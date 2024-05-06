package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class Dereference extends Argument {

    public final Argument inner;

    public Dereference(Argument inner) {
        super(inner.token, Type.DEREFERENCE);
        this.inner = inner;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return inner.size(sectionLocator);
    }

    @Override
    public void encode(ILabelResolver labelResolver, WritableByteChannel channel, int instructionAddress) throws IOException {
        inner.encode(labelResolver, channel, instructionAddress);
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        String innerEncoding = inner.getImmediateEncoding(sectionLocator);
        switch(innerEncoding) {
            case "cst":
                return "page0";
            case "dcst":
                return "abs";
            case "rcrd":
                return "ircrd";
            default:
                throw new RuntimeException("invalid dereference argument: " + inner.type);
        }
    }
}
