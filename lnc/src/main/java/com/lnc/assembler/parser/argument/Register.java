package com.lnc.assembler.parser.argument;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.parser.RegisterId;

/**
 * Represents a register in an assembly language context. This class extends the
 * {@code Argument} class to define a concrete type of argument that refers to a register.
 * It encapsulates a {@code RegisterId}, which uniquely identifies the register being referenced.
 *
 * The {@code Register} class provides the following functionality:
 * - Determines the size of the encoded register representation.
 * - Encodes the register into a binary format.
 * - Provides a string representation of the register.
 * - Retrieves the immediate encoding representation of the register.
 * - Compares the equality of two {@code Register} objects.
 *
 * A register is constructed by parsing a token's lexeme and resolving it to a valid {@code RegisterId}.
 */
public class Register extends Argument {
    public final RegisterId reg;

    public Register(Token token) {
        super(token, Type.REGISTER);
        this.reg = RegisterId.fromString(token.lexeme);
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return 0;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {
        return new byte[0];
    }

    @Override
    public String toString() {
        return reg.toString();
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return reg.toString().toLowerCase();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Register other)) return false;
        return reg == other.reg;
    }


}
