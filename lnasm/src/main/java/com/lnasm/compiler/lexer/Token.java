package com.lnasm.compiler.lexer;

public class Token {

    public Location location;
    public Token macroSub;

    public final Type type;
    public final String lexeme;
    public final Object literal;


    public Token(Type type, String lexeme, Object literal, Location location) {
        this(null, type, lexeme, literal, location);
    }

    public Token(Token macroSub, Type type, String lexeme, Object literal, Location location) {
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

    public String formatLocation(){
        return String.format("%s:%d:%d", this.location.filename, this.location.lineNumber, this.location.colNumber);
    }

    public enum Type {
        MACRO_DEFINE,
        MACRO_UNDEFINE,
        MACRO_INCLUDE,
        MACRO_IFDEF,
        MACRO_IFNDEF,
        MACRO_ENDIF,

        MACRO_ERROR,

        DIR_ORG,
        DIR_DATA,
        DIR_RES,

        L_SQUARE_BRACKET,
        R_SQUARE_BRACKET,
        IDENTIFIER,
        INTEGER,
        STRING,
        COMMA,
        COLON,
        SEMICOLON,
        EQUALS,
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
        SP,

        // Linker config keywords
        SECTIONS;
        public static final Token.Type[] LNASM_KEYWORDSET = new Token.Type[]{
                Token.Type.NOP,
                Token.Type.HLT,
                Token.Type.MOV,
                Token.Type.PUSH,
                Token.Type.POP,
                Token.Type.ADD,
                Token.Type.SUB,
                Token.Type.CMP,
                Token.Type.AND,
                Token.Type.OR,
                Token.Type.XOR,
                Token.Type.SWAP,
                Token.Type.NOT,
                Token.Type.DEC,
                Token.Type.INC,
                Token.Type.SHL,
                Token.Type.SHR,
                Token.Type.JC,
                Token.Type.JZ,
                Token.Type.JN,
                Token.Type.GOTO,
                Token.Type.LJC,
                Token.Type.LJZ,
                Token.Type.LJN,
                Token.Type.LGOTO,
                Token.Type.LCALL,
                Token.Type.RET,
                Token.Type.IRET,
                Token.Type.SID,
                Token.Type.CID,
                Token.Type.BRK,
                Token.Type.RA,
                Token.Type.RB,
                Token.Type.RC,
                Token.Type.RD,
                Token.Type.SS,
                Token.Type.SP,
        };

        public static final Token.Type[] LINKER_CONFIG_KEYWORDSET = new Token.Type[]{
                Token.Type.SECTIONS
        };
    }

}
