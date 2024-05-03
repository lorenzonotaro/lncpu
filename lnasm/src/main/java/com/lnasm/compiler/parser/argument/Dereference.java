package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.ILabelSectionLocator;
import com.lnasm.compiler.linker.AbstractLinker;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class Dereference extends Argument {

    public final Argument inner;

    public Dereference(Argument inner) {
        super(inner.token, Type.DEREFERENCE);
        this.inner = inner;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        return inner.size(sectionLocator, linker);
    }

    @Override
    public void encode(ILabelSectionLocator sectionLocator, AbstractLinker linker, WritableByteChannel channel) throws IOException {
        inner.encode(sectionLocator, linker, channel);
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        String innerEncoding = inner.getImmediateEncoding(sectionLocator, linker);
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
