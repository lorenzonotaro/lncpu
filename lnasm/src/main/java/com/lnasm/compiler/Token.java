package com.lnasm.compiler;

public class Token {
    public String file;
    Token macroSub;
    final Type type;
    public final String lexeme;
    public final Object literal;
    public final int line;
    public final int col;

    public Token(Type type, String lexeme, Object literal, String file, int line, int col) {
        this(null, type, lexeme, literal, file, line, col);
    }

    public enum Type{
        MACRO_DEFINE,
        MACRO_UNDEFINE,
        MACRO_INCLUDE,
        MACRO_IFDEF,
        MACRO_IFNDEF,
        MACRO_ENDIF,

        DIR_ORG,
        DIR_DATA,
        DIR_PAD,

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
        SWAP,
        NOT,
        DEC,
        INC,
        SHL,
        SHR,
        JC,
        JZ,
        JN,
        GOTO,
        LJC,
        LJZ,
        LJN,
        LGOTO,
        LCALL,
        RET,

        IRET,

        SID,

        CID,

        BRK,
        //registers
        RA,
        RB,
        RC,
        RD,
        SS,
        SP
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

    public String formatLocation(){
        return String.format("%s:%d:%d", this.file, this.line, this.col);
    }
}
