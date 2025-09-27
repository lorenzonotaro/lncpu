package com.lnc.assembler.parser.argument;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

/**
 * Represents a 16-bit numerical argument referred to as a "word".
 *
 * This class extends {@code NumericalArgument} and provides functionality
 * specific to 16-bit numerical arguments used in assembly or linking processes.
 * A word is a two-byte value, usually represented in hexadecimal format.
 *
 * Key features of the {@code Word} class include:
 * - Construction from a {@code Token} object, validating that the value falls within the
 *   range of a 16-bit signed integer or an unsigned 16-bit integer.
 * - Ability to encode the word as a two-byte array for use in machine instructions.
 * - Methods to determine the size of the word and its immediate encoding format.
 */
public class Word extends NumericalArgument {
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
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {
        return new byte[]{
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    @Override
    public String toString() {
        return String.format("0x%04x", value & 0xFFFF);
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return "dcst";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Word other)) return false;
        return value == other.value;
    }

    @Override
    public int value(ILabelResolver labelResolver, int instructionAddress) {
        return value;
    }
}
