package com.lnc.cc.types;

import com.lnc.cc.parser.LncParser;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

public abstract class TypeSpecifier {

    public final Type type;

    public static final TokenType[] VALID_TOKENS = new TokenType[]{
        TokenType.VOID,
        TokenType.CHAR,
        TokenType.I8,
        TokenType.UI8,
        TokenType.STRUCT,
        TokenType.BOOL
    };

    public TypeSpecifier(Type type){
        this.type = type;
    }

    public static TypeSpecifier parsePrimaryType(LncParser parser) {
        Token token = parser.consume("Expected type specifier", VALID_TOKENS);
        switch(token.type){
            case VOID:
                return new VoidType();
            case CHAR:
                return new CharType();
            case I8:
                return new I8Type();
            case UI8:
                return new UI8Type();
            case STRUCT:
                return new StructType(parser.consume("expected struct name", TokenType.IDENTIFIER).lexeme);
            case BOOL:
                return new BoolType();
            default:
                return null;
        }
    }

    public enum Type{
        VOID,
        CHAR,
        I8,
        UI8,
        STRUCT,
        BOOL,
        POINTER
    }

}
