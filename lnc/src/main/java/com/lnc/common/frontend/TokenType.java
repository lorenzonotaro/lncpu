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
            TokenType.SWAP,
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
            TokenType.SWAP,
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
}
