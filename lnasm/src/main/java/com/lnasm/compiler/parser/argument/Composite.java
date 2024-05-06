package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;
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
    public int size(ILabelSectionLocator sectionLocator) {
        return high.size(sectionLocator) + low.size(sectionLocator);
    }

    @Override
    public void encode(ILabelResolver labelResolver, WritableByteChannel channel, int instructionAddress) throws IOException {
        high.encode(labelResolver, channel, instructionAddress);
        low.encode(labelResolver, channel, instructionAddress);
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        if(high.type == Type.BYTE && low.type == Type.BYTE)
            return "dcst";
        else if(high.type == Type.REGISTER && low.type == Type.REGISTER && ((Register)high).reg == RegisterId.RC && ((Register)low).reg == RegisterId.RD)
            return "rcrd";
        throw new CompileException("Invalid composite argument: " + high.type + ", " + low.type, token);
    }
}
