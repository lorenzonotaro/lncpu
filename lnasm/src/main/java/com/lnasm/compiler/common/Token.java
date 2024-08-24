package com.lnasm.compiler.common;

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

    public static Token __internal(Type type, Object literal){
        String lexeme = "";

        switch(type){
            case STRING:
                lexeme = "\n%s\n".formatted(literal);
                break;
            case INTEGER:
                lexeme = literal.toString();
                break;
            default:
                break;
        }

        return new Token(type, lexeme, literal, new Location("<internal>", "<interal>", lexeme, 0, 0));
    }

    public String formatLocation(){
        return String.format("%s:%d:%d", this.location.filename, this.location.lineNumber, this.location.colNumber);
    }

    public short ensureShort(){
        if(type == Type.INTEGER){
            int value = (Integer) literal;
            if(IntUtils.inShortRange(value)){
                return (short) value;
            }
        }
        throw new CompileException("value out of range for word", this);
    }

    public byte ensureByte(){
        if(type == Type.INTEGER){
            int value = (Integer) literal;
            if(IntUtils.inByteRange(value)){
                return (byte) value;
            }
        }
        throw new CompileException("value out of range for byte", this);
    }

    public enum Type {
        MACRO_DEFINE,
        MACRO_UNDEFINE,
        MACRO_INCLUDE,
        MACRO_IFDEF,
        MACRO_IFNDEF,
        MACRO_ENDIF,

        MACRO_ERROR,

        DIR_SECTION,
        DIR_DATA,
        DIR_RES,

        L_PAREN,
        R_PAREN,

        L_SQUARE_BRACKET,
        R_SQUARE_BRACKET,
        IDENTIFIER,
        INTEGER,
        STRING,
        COMMA,
        COLON,
        DOUBLE_COLON,
        SEMICOLON,

        //operators
        PLUS,

        MINUS,

        STAR,

        SLASH,

        BITWISE_LEFT,

        BITWISE_RIGHT,

        BITWISE_AND,

        BITWISE_OR,

        BITWISE_XOR,

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

        CLC,

        SEC,

        BRK,
        //registers
        RA,
        RB,
        RC,
        RD,
        SS,
        SP,
        DS,

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
                Token.Type.CLC,
                Token.Type.SEC,
                Token.Type.BRK,
                Token.Type.RA,
                Token.Type.RB,
                Token.Type.RC,
                Token.Type.RD,
                Token.Type.SS,
                Token.Type.SP,
                Token.Type.DS
        };

        public static final Token.Type[] LNASM_INSTRUCTIONSET = new Token.Type[]{
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
                Token.Type.CLC,
                Token.Type.SEC,
                Token.Type.BRK
        };



        public static final Token.Type[] LINKER_CONFIG_KEYWORDSET = new Token.Type[]{
                Token.Type.SECTIONS
        };
    }

}
