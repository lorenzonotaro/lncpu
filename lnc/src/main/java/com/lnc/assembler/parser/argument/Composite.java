package com.lnc.assembler.parser.argument;

import com.lnc.common.frontend.CompileException;
import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.linker.LinkInfo;
import com.lnc.assembler.parser.RegisterId;

import java.io.IOException;

public class Composite extends Argument {
    public final Argument high, low;

    public Composite(Argument high, Argument low) {
        super(high.token, Type.COMPOSITE);
        this.high = high;
        this.low = low;

        if(!((high.type == Type.BYTE && low.type == Type.BYTE) ||
                high.type == Type.REGISTER && low.type == Type.REGISTER && ((Register)high).reg == RegisterId.RC && ((Register)low).reg == RegisterId.RD))
            throw new CompileException("Invalid composite argument: " + high.type + ", " + low.type, token);
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return high.size(sectionLocator) + low.size(sectionLocator);
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        byte[] highBytes = high.encode(labelResolver, linkInfo, instructionAddress);
        byte[] lowBytes = low.encode(labelResolver, linkInfo, instructionAddress);

        byte[] result = new byte[highBytes.length + lowBytes.length];
        System.arraycopy(highBytes, 0, result, 0, highBytes.length);
        System.arraycopy(lowBytes, 0, result, highBytes.length, lowBytes.length);
        return result;
    }

    @Override
    public String toString() {
        return high.toString() +
                ":" +
                low.toString();
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
