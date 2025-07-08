package com.lnc.assembler.parser.argument;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.linker.LinkInfo;
import com.lnc.assembler.parser.RegisterId;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.io.IOException;

public class RegisterOffset extends Argument{

    public final Register register;

    public final NumericalArgument offset;
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
    public int size(ILabelSectionLocator sectionLocator) {
        return 1;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {

        var val = offset.value(labelResolver, linkInfo, instructionAddress);

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
