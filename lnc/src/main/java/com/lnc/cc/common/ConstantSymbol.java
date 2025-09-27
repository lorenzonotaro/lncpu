package com.lnc.cc.common;

import com.lnc.assembler.parser.EncodedData;
import com.lnc.cc.types.CharType;
import com.lnc.cc.types.PointerType;
import com.lnc.cc.types.TypeQualifier;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

/**
 * Represents a constant symbol, which is a subclass of {@code BaseSymbol},
 * used to define entities that have a fixed encoded value, such as string literals
 * or numeric constants.
 *
 * The {@code ConstantSymbol} class is immutable and includes additional functionality
 * to associate a constant value of type {@code EncodedData} with the symbol.
 */
public class ConstantSymbol extends BaseSymbol {

    private final EncodedData value;

    protected ConstantSymbol(Token token, TypeSpecifier typeSpecifier, EncodedData value) {
        super(token, typeSpecifier, TypeQualifier.NONE, false, -1);
        this.value = value;
    }

    public EncodedData getValue() {
        return value;
    }

    public static ConstantSymbol string(Token stringToken){
        return new ConstantSymbol(stringToken, new PointerType(new CharType(), PointerType.PointerKind.FAR), EncodedData.ofString(stringToken.literal.toString()));
    }
}
