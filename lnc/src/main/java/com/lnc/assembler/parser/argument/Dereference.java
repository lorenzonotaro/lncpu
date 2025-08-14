package com.lnc.assembler.parser.argument;

import com.lnc.common.frontend.CompileException;
import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.linker.LinkInfo;

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
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        return inner.encode(labelResolver, linkInfo, instructionAddress);
    }

    @Override
    public String toString() {
        return "[" + inner.toString() + "]";
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        String innerEncoding = inner.getImmediateEncoding(sectionLocator);
        switch(innerEncoding) {
            case "cst":
                return "datap";
            case "dcst":
                return "abs";
            case "rcrd":
                return "ircrd";
            case "rd":
                return "ird";
            case "bpoffset":
                return "ibpoffset";
            default:
                throw new CompileException("invalid dereference argument: " + inner.type, inner.token);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Dereference other)) return false;
        return inner.equals(other.inner);
    }
}
