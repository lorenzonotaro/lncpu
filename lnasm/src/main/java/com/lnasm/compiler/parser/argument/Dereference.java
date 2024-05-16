package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;

import java.io.IOException;

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
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) throws IOException {
        return inner.encode(labelResolver, instructionAddress);
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
                throw new CompileException("invalid dereference argument: " + inner.type, inner.token);
        }
    }
}
