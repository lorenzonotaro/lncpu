package com.lnc.cc.types;

import com.lnc.cc.parser.LncParser;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

public abstract class TypeSpecifier {

    public final Type type;

    public static final TokenType[] VALID_TOKENS = new TokenType[]{
        TokenType.VOID,
        TokenType.CHAR,
        TokenType.T_INT,
        TokenType.UNSIGNED,
        TokenType.SIGNED,
        TokenType.STRUCT
    };

    public TypeSpecifier(Type type){
        this.type = type;
    }

    public static TypeSpecifier parsePrimaryType(LncParser parser) {
        Token token;
        Type type = null;
        boolean signed = false;
        boolean unsigned = false;
        while(parser.check(VALID_TOKENS)){
            switch((token = parser.advance()).type){
                case UNSIGNED:
                    if(unsigned) throw new CompileException("duplicate 'unsigned' qualifier", token);
                    if(signed) throw new CompileException("cannot have both 'signed' and 'unsigned' qualifiers", token);
                    unsigned = true;
                break;
                case SIGNED:
                    if(signed) throw new CompileException("duplicate 'signed' qualifier", token);
                    if(unsigned) throw new CompileException("cannot have both 'signed' and 'unsigned' qualifiers", token);
                    signed = true;
                break;
                case CHAR:
                    if(type != null) throw new CompileException("duplicate type specifier", token);
                    type = Type.CHAR;
                break;
                case T_INT:
                    if(type != null) throw new CompileException("duplicate type specifier", token);
                    type = Type.I8;
                break;
                case VOID:
                    if(type != null) throw new CompileException("duplicate type specifier", token);
                    type = Type.VOID;
                    break;
                default:
                    throw new CompileException("Invalid type specifier: " + token, token);
            }
        }

        if(type == null) {
            throw new CompileException("Missing type specifier", parser.peek());
        }

        return switch (type) {
            case CHAR -> new CharType();
            case I8 -> unsigned ? new UI8Type() : new I8Type();
            case VOID -> new VoidType();
            default -> throw new Error("Invalid type specifier: " + type);
        };
    }

    public enum Type{
        VOID,
        CHAR,
        I8,
        UI8,
        POINTER
    }

}
