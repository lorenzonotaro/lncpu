package com.lnc.assembler.parser.argument;

import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.linker.LinkInfo;

import java.io.IOException;

public class NumberCast extends NumericalArgument{

    private final NumericalArgument source;

    private final int targetSize;

    private final Token targetType;

    public NumberCast(Argument source, Token castToken, Token targetType) {
        super(castToken, Type.CAST);


        if (!(source instanceof NumericalArgument)){
            throw new CompileException("cannot cast non-numerical type", targetType);
        }

        this.source = (NumericalArgument) source;

        this.targetType = targetType;

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
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        byte[] sourceEncoded = source.encode(labelResolver, linkInfo, instructionAddress);
        byte[] result = new byte[targetSize];

        for (int i = 0; i < targetSize; i++) {
            result[targetSize - 1 - i] =
                    i < sourceEncoded.length ? sourceEncoded[sourceEncoded.length - 1 - i] : 0;
        }

        return result;
    }

    @Override
    public String toString() {
        return "(" + source.toString() + ")::" + targetType.lexeme;
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return switch(targetSize){
            case 1 -> "cst";
            case 2 -> "dcst";
            default -> throw new CompileException("invalid cast target size", token);
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NumberCast other)) return false;
        return source.equals(other.source) && targetSize == other.targetSize && targetType.equals(other.targetType);
    }

    @Override
    public int value(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        if (targetSize == 1) {
            return source.value(labelResolver, linkInfo, instructionAddress) & 0xFF; // Cast to byte
        } else if (targetSize == 2) {
            return source.value(labelResolver, linkInfo, instructionAddress) & 0xFFFF; // Cast to word
        } else {
            throw new CompileException("invalid cast target size", token);
        }
    }
}
