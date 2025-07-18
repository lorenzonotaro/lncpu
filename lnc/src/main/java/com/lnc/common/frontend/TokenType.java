package com.lnc.common.frontend;

public enum TokenType {
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

    L_CURLY_BRACE,
    R_CURLY_BRACE,

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

    DOUBLE_PLUS,

    MINUS,

    DOUBLE_MINUS,

    STAR,

    SLASH,

    BITWISE_LEFT,

    BITWISE_RIGHT,

    AMPERSAND,

    BITWISE_OR,

    BITWISE_XOR,

    BITWISE_NOT,

    LOGICAL_AND,

    LOGICAL_OR,

    LOGICAL_NOT,

    EQUALS,

    NOT_EQUALS,

    DOUBLE_EQUALS,

    GREATER_THAN,

    LESS_THAN,

    GREATER_THAN_OR_EQUAL,

    LESS_THAN_OR_EQUAL,

    ARROW,

    DOT,

    //instructions
    NOP,
    HLT,
    INT,
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
    BP,
    DS,

    // Linker config keywords
    SECTIONS,

    // lnc keywords
    T_INT("int"),
    UNSIGNED("unsigned"),
    SIGNED("signed"),
    CHAR("char"),
    DPAGE("dpage"),
    VOID("void"),
    STRUCT("struct"),
    IF("if"),
    ELSE("else"),
    WHILE("while"),
    DO("do"),
    FOR("for"),
    RETURN("return"),
    BREAK("break"),
    CONTINUE("continue"),
    SWITCH("switch"),
    CASE("case"),
    DEFAULT("default"),
    TYPEDEF("typedef"),
    SIZEOF("sizeof"),
    STATIC("static"),
    EXTERN("extern"),
    CONST("const"),
    NEAR("near"),
    FAR("far");

    public static final TokenType[] LNASM_KEYWORDSET = new TokenType[]{
            TokenType.NOP,
            TokenType.HLT,
            TokenType.INT,
            TokenType.MOV,
            TokenType.PUSH,
            TokenType.POP,
            TokenType.ADD,
            TokenType.SUB,
            TokenType.CMP,
            TokenType.AND,
            TokenType.OR,
            TokenType.XOR,
            TokenType.NOT,
            TokenType.DEC,
            TokenType.INC,
            TokenType.SHL,
            TokenType.SHR,
            TokenType.JC,
            TokenType.JZ,
            TokenType.JN,
            TokenType.GOTO,
            TokenType.LJC,
            TokenType.LJZ,
            TokenType.LJN,
            TokenType.LGOTO,
            TokenType.LCALL,
            TokenType.RET,
            TokenType.IRET,
            TokenType.SID,
            TokenType.CID,
            TokenType.CLC,
            TokenType.SEC,
            TokenType.BRK,
            TokenType.RA,
            TokenType.RB,
            TokenType.RC,
            TokenType.RD,
            TokenType.SS,
            TokenType.SP,
            TokenType.BP,
            TokenType.DS
    };

    public static final TokenType[] LNASM_INSTRUCTIONSET = new TokenType[]{
            TokenType.NOP,
            TokenType.HLT,
            TokenType.INT,
            TokenType.MOV,
            TokenType.PUSH,
            TokenType.POP,
            TokenType.ADD,
            TokenType.SUB,
            TokenType.CMP,
            TokenType.AND,
            TokenType.OR,
            TokenType.XOR,
            TokenType.NOT,
            TokenType.DEC,
            TokenType.INC,
            TokenType.SHL,
            TokenType.SHR,
            TokenType.JC,
            TokenType.JZ,
            TokenType.JN,
            TokenType.GOTO,
            TokenType.LJC,
            TokenType.LJZ,
            TokenType.LJN,
            TokenType.LGOTO,
            TokenType.LCALL,
            TokenType.RET,
            TokenType.IRET,
            TokenType.SID,
            TokenType.CID,
            TokenType.CLC,
            TokenType.SEC,
            TokenType.BRK
    };


    public static final TokenType[] LINKER_CONFIG_KEYWORDSET = new TokenType[]{
            TokenType.SECTIONS
    };

    public static final TokenType[] LNC_KEYWORDSET = new TokenType[]{
            TokenType.T_INT,
            TokenType.UNSIGNED,
            TokenType.SIGNED,
            TokenType.CHAR,
            TokenType.DPAGE,
            TokenType.VOID,
            TokenType.STRUCT,
            TokenType.IF,
            TokenType.ELSE,
            TokenType.WHILE,
            TokenType.DO,
            TokenType.FOR,
            TokenType.RETURN,
            TokenType.BREAK,
            TokenType.CONTINUE,
            TokenType.SWITCH,
            TokenType.CASE,
            TokenType.DEFAULT,
            TokenType.TYPEDEF,
            TokenType.SIZEOF,
            TokenType.STATIC,
            TokenType.EXTERN,
            TokenType.CONST,
            TokenType.NEAR,
            TokenType.FAR
    };

    private final String strValue;

    TokenType(String strValue) {
        this.strValue = strValue;
    }

    TokenType(){
        this.strValue = name();
    }

    public String getStrValue() {
        return strValue;
    }
}
