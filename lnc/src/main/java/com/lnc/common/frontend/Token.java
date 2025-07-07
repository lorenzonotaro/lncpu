package com.lnc.common.frontend;

import com.lnc.common.IntUtils;

public class Token {

    public Location location;
    public Token macroSub;

    public final TokenType type;
    public final String lexeme;
    public final Object literal;


    public Token(TokenType type, String lexeme, Object literal, Location location) {
        this(null, type, lexeme, literal, location);
    }

    public Token(Token macroSub, TokenType type, String lexeme, Object literal, Location location) {
        this.macroSub = macroSub;
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.location = location;
    }

    public Token(Token token, Token macroSub){
        this(macroSub, token.type, token.lexeme, token.literal, token.location);
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", lexeme='" + lexeme + '\'' +
                ", literal=" + literal +
                ", line=" + location.lineNumber +
                ", col=" + location.colNumber +
                '}';
    }

    public static Token __internal(TokenType type, Object literal){
        String lexeme = "";

        switch(type){
            case STRING:
                lexeme = "\n%s\n".formatted(literal);
                break;
            default:
                lexeme = literal.toString();
                break;
        }

        return new Token(type, lexeme, literal, new Location("<internal>", "<interal>", lexeme, 0, 0));
    }

    public String formatLocation(){
        return String.format("%s:%d:%d", this.location.filename, this.location.lineNumber, this.location.colNumber);
    }

    public short ensureShort(){
        if(type == TokenType.INTEGER){
            int value = (Integer) literal;
            if(IntUtils.inShortRange(value)){
                return (short) value;
            }
        }
        throw new CompileException("value out of range for word", this);
    }

    public byte ensureByte(){
        if(type == TokenType.INTEGER){
            int value = (Integer) literal;
            if(IntUtils.inByteRange(value)){
                return (byte) value;
            }
        }
        throw new CompileException("value out of range for byte", this);
    }

}
