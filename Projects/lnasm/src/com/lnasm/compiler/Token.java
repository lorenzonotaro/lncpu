package com.lnasm.compiler;

public class Token {
    public String file;
    Token macroSub;
    final Type type;
    public final String lexeme;
    public final Object literal;
    final int line;
    final int col;

    public Token(Type type, String lexeme, Object literal, String file, int line, int col) {
        this(null, type, lexeme, literal, file, line, col);
    }

    public enum Type{
        MACRO_DEFINE,
        MACRO_UNDEFINE,
        MACRO_INCLUDE,
        DIR_SEGMENT,
        DIR_DATA,
        L_SQUARE_BRACKET,
        R_SQUARE_BRACKET,
        IDENTIFIER,
        INTEGER,
        STRING,
        COMMA,
        COLON,
        //instructions
        NOP,
        HLT,
        MOV,
        PUSH,
        POP,
        ADD,
        SUB,
        CMP,
        AND,
        OR,
        XOR,
        NOT,
        SHL,
        SHR,
        JC,
        JZ,
        JN,
        JA,
        GOTO,
        CALL,
        RET,
        POLL,
        TSM,
        RFL,
        //registers
        RA,
        RB,
        RC,
        RD,
        MDS,
        SS,
        SP,
        SDS,
        //other keywords
        RAM,
        ROM;
    }

    public Token(Token macroSub, Type type, String lexeme, Object literal, String file, int line, int col) {
        this.macroSub = macroSub;
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.file = file;
        this.line = line;
        this.col = col;
    }

    public Token(Token token, Token macroSub){
        this(macroSub, token.type, token.lexeme, token.literal, token.file, token.line, token.col);
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", lexeme='" + lexeme + '\'' +
                ", literal=" + literal +
                ", line=" + line +
                ", col=" + col +
                '}';
    }
}
