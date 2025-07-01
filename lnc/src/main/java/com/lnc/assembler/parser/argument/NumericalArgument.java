package com.lnc.assembler.parser.argument;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.LinkInfo;
import com.lnc.common.frontend.Token;

public abstract class NumericalArgument extends Argument {

    public NumericalArgument(Token token, Type type) {
        super(token, type);
    }

    public abstract int value(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress);
}
