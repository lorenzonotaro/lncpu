package com.lnc.assembler.parser.argument;

import com.lnc.common.frontend.CompileException;
import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.parser.RegisterId;

/**
 * Represents a composite argument, which consists of two sub-arguments, `high` and `low`.
 * This class extends the `Argument` class and is used to represent arguments composed of
 * a high-level and a low-level part, such as composite registers or byte pairs.
 *
 * The composite argument must satisfy specific validation rules depending on the types
 * of its `high` and `low` components. If the validation fails, a `CompileException` is thrown.
 */
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
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {
        byte[] highBytes = high.encode(labelResolver, instructionAddress);
        byte[] lowBytes = low.encode(labelResolver, instructionAddress);

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Composite other)) return false;
        return high.equals(other.high) && low.equals(other.low);
    }
}
