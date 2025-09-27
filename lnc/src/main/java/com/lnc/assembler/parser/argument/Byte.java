package com.lnc.assembler.parser.argument;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.common.frontend.Token;

/**
 * Represents a byte-sized numerical argument in assembly or machine code, inheriting from {@code NumericalArgument}.
 * This class is used for handling and encoding byte values in specific contexts, such as instructions or operands.
 *
 * A {@code Byte} object is instantiated using a {@code Token} that contains the byte value. The byte value
 * is validated and enforced during the object's construction.
 *
 * Key Features:
 * - The {@code Byte} class overrides methods for determining its size, encoding its value, and providing
 *   specific string and equality representations.
 * - Encodes itself as a single byte.
 * - Implements operations to retrieve its integer equivalent and its immediate encoding type.
 *
 * Core Methods:
 * - {@code size(ILabelSectionLocator)}: Returns the size of the byte, always 1.
 * - {@code encode(ILabelResolver, int)}: Encodes the byte value into its raw byte array form.
 * - {@code toString()}: Produces a string in hexadecimal representation for the byte value.
 * - {@code getImmediateEncoding(ILabelSectionLocator)}: Returns the identifier for the immediate encoding type.
 * - {@code equals(Object)}: Compares byte values for equality.
 * - {@code value(ILabelResolver, int)}: Returns the unsigned integer representation of the byte.
 */
public class Byte extends NumericalArgument {
    public byte value;

    public Byte(Token token) {
        super(token, Type.BYTE);
        this.value = token.ensureByte();
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return 1;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {
        return new byte[] { value };
    }

    @Override
    public String toString() {
        return String.format("0x%02x", value & 0xFF);
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return "cst";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Byte other)) return false;
        return value == other.value;
    }

    @Override
    public int value(ILabelResolver labelResolver, int instructionAddress) {
        return value & 0xFF;
    }
}
