package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.ILabelSectionLocator;
import com.lnasm.compiler.linker.AbstractLinker;
import com.lnasm.compiler.parser.RegisterId;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public class Composite extends Argument {
    public final Argument high, low;

    public Composite(Argument high, Argument low) {
        super(high.token, Type.COMPOSITE);
        this.high = high;
        this.low = low;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        return high.size(sectionLocator, linker) + low.size(sectionLocator, linker);
    }

    @Override
    public void encode(ILabelSectionLocator sectionLocator, AbstractLinker linker, WritableByteChannel channel) throws IOException {
        high.encode(sectionLocator, linker, channel);
        low.encode(sectionLocator, linker, channel);
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        if(high.type == Type.BYTE && low.type == Type.BYTE)
            return "dcst";
        else if(high.type == Type.REGISTER && low.type == Type.REGISTER && ((Register)high).reg == RegisterId.RC && ((Register)low).reg == RegisterId.RD)
            return "rcrd";
        throw new CompileException("Invalid composite argument: " + high.type + ", " + low.type, token);
    }
}
