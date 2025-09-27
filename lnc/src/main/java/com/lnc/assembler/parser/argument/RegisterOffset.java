package com.lnc.assembler.parser.argument;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.parser.RegisterId;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

/**
 * Represents a register offset argument in an assembly language context. This class extends
 * the {@code Argument} class and is used to model an offset applied to a specific
 * register. The offset is represented as a numerical value, and a valid operator
 * (either addition or subtraction) connects the register and the offset.
 *
 * A {@code RegisterOffset} is constructed by providing a base register, an operator
 * token, and a numerical argument representing the offset. The class ensures that:
 * - The provided base register is of type {@code REGISTER}.
 * - The base register is specifically the FP (Frame Pointer) register.
 * - The operator is either the '+' or '-' token.
 * These constraints are strictly enforced to adhere to valid assembly semantics.
 *
 * Key functionalities of this class include:
 * - Encapsulation of the register, operator, and offset into a cohesive object.
 * - Overriding the string representation for accurate textual representations.
 * - Encoding the register offset into a machine-readable binary format.
 * - Validation of the offset to ensure it falls within the byte range (-128 to 127).
 * - Size determination for memory alignment during encoding.
 *
 * This class also supports equality comparison by checking the equality of the
 * constituent register, offset, and operator components.
 */
public class RegisterOffset extends Argument{

    public final Register register;

    public NumericalArgument offset;
    private final Token operator;

    public RegisterOffset(Argument register, Token operator, NumericalArgument offset) {
        super(register.token, Type.REGISTER_OFFSET);
        this.offset = offset;

        if (register.type != Type.REGISTER) {
            throw new IllegalArgumentException("Invalid register type: " + register.type);
        }

        this.register = (Register) register;

        if(this.register.reg != RegisterId.BP) {
            throw new IllegalArgumentException("Register offset can only be applied to FP register, not: " + this.register.reg);
        }

        if(!(operator.type == TokenType.PLUS || operator.type == TokenType.MINUS)) {
            throw new IllegalArgumentException("Invalid operator for register offset: " + operator.type);
        }

        this.operator = operator;

    }

    @Override
    public String toString() {
        return register.toString() + " " + operator.lexeme + " " + offset.toString();
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return register.getImmediateEncoding(sectionLocator) + "offset";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RegisterOffset other)) return false;
        return register.equals(other.register) && offset.equals(other.offset) && Token.equivalent(operator, other.operator);
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return 1;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {

        var val = offset.value(labelResolver, instructionAddress);

        if(operator.type == TokenType.MINUS) {
            val = -val;
        }

        if (!IntUtils.inByteRange(val)) {
            throw new IllegalArgumentException("Offset must be in byte range: " + val);
        }

        return new byte[] {
                (byte) (val & 0xFF)
        };
    }

    public Token getOperator() {
        return operator;
    }
}
