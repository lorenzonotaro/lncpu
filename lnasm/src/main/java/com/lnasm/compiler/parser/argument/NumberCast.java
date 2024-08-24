package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;
import com.lnasm.compiler.linker.LinkInfo;

import java.io.IOException;

public class NumberCast extends Argument{

    private final Argument source;

    private final int targetSize;

    public NumberCast(Argument source, Token castToken, Token targetType) {
        super(castToken, Type.CAST, true);

        this.source = source;

        if (!source.numerical){
            throw new CompileException("cannot cast non-numerical type", targetType);
        }

        switch (targetType.lexeme) {
            case "byte", "8", "cst" -> targetSize = 1;
            case "word", "16", "dcst" -> targetSize = 2;
            default -> throw new CompileException("invalid cast target type", targetType);
        }
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return targetSize;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) throws IOException {
        byte[] sourceEncoded = source.encode(labelResolver, linkInfo, instructionAddress);
        byte[] result = new byte[targetSize];

        for (int i = 0; i < targetSize; i++) {
            result[targetSize - 1 - i] =
                    i < sourceEncoded.length ? sourceEncoded[sourceEncoded.length - 1 - i] : 0;
        }

        return result;
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return switch(targetSize){
            case 1 -> "cst";
            case 2 -> "dcst";
            default -> throw new CompileException("invalid cast target size", token);
        };
    }
}
