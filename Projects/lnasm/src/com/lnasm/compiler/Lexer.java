package com.lnasm.compiler;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final Token macroSubToken;
    private List<List<Token>> lines;
    private Line line;
    private int index;
    private int start;

    Lexer() {
        this(null);
    }

    Lexer(Token macroSubToken) {
        this.macroSubToken = macroSubToken;
        lines = new ArrayList<>();
    }

    boolean parse(List<Line> sourceLines){
        lines.clear();
        boolean success = true;
        this.lines = new ArrayList<>();
        for (Line line : sourceLines) {
            try {
                List<Token> tokens = this.parseLine(line);
                lines.add(tokens);
            }catch (CompileException e){
                e.log();
                success = false;
            }
        }
        return success;
    }

    public List<List<Token>> getLines(){
        return lines;
    }

    List<Token> parseLine(Line line) {
        reset(line);

        List<Token> tokens = new ArrayList<>(10);

        Token t;
        while (!isAtEnd() && (t = match(advance())) != null) {
            tokens.add(t);
            start = index;
        }

        return tokens;
    }

    private Token match(char c) {
        switch (c) {
            case '\t':
            case ' ':
            case '\r':
            case '\b':
                start = index;
                return match(advance());
            case '\0':
            case ';':
                return null;
            case '.':
                return directive();
            case '%':
                return macro();
            case '[':
                return token(Token.Type.L_SQUARE_BRACKET);
            case ']':
                return token(Token.Type.R_SQUARE_BRACKET);
            case ',':
                return token(Token.Type.COMMA);
            case ':':
                return token(Token.Type.COLON);
            case '"':
            case '\'':
                return string(c);
            default:
                if (Character.isLetter(c) || c == '_')
                    return name();
                if (c == '0') {
                    c = peek();
                    if (c == 'x')
                        return integer(16);
                    else if (c == 'b')
                        return integer(2);
                    else return integer(10);
                }
                if (Character.isDigit(c))
                    return integer(10);
                throw error("unexpected character", String.valueOf(c));
        }
    }

    private Token name() {
        String ident = identifier();
        switch (ident.toLowerCase()) {
            case "nop":
                return token(Token.Type.NOP);
            case "hlt":
                return token(Token.Type.HLT);
            case "mov":
                return token(Token.Type.MOV);
            case "push":
                return token(Token.Type.PUSH);
            case "pop":
                return token(Token.Type.POP);
            case "add":
                return token(Token.Type.ADD);
            case "sub":
                return token(Token.Type.SUB);
            case "cmp":
                return token(Token.Type.CMP);
            case "AND":
                return token(Token.Type.AND);
            case "or":
                return token(Token.Type.OR);
            case "xor":
                return token(Token.Type.XOR);
            case "not":
                return token(Token.Type.NOT);
            case "shl":
                return token(Token.Type.SHL);
            case "shr":
                return token(Token.Type.SHR);
            case "jc":
                return token(Token.Type.JC);
            case "jz":
                return token(Token.Type.JZ);
            case "jn":
                return token(Token.Type.JN);
            case "goto":
                return token(Token.Type.GOTO);
            case "ljc":
                return token(Token.Type.LJC);
            case "ljz":
                return token(Token.Type.LJZ);
            case "ljn":
                return token(Token.Type.LJN);
            case "lgoto":
                return token(Token.Type.LGOTO);
            case "lcall":
                return token(Token.Type.LCALL);
            case "ret":
                return token(Token.Type.RET);
            case "iret":
                return token(Token.Type.IRET);
            case "sid":
                return token(Token.Type.SID);
            case "cid":
                return token(Token.Type.CID);
            case "brk":
                return token(Token.Type.CID);
            case "ra":
                return token(Token.Type.RA);
            case "rb":
                return token(Token.Type.RB);
            case "rc":
                return token(Token.Type.RC);
            case "rd":
                return token(Token.Type.RD);
            case "ss":
                return token(Token.Type.SS);
            case "sp":
                return token(Token.Type.SP);
            default:
                return token(Token.Type.IDENTIFIER, ident);
        }
    }

    private CompileException error(String message, String lexeme) {
        return new CompileException(message, lexeme, line.filename, line.number, start + 1);
    }

    private Token string(char terminator) {
        while (!isAtEnd() && peek() != terminator)
            advance();

        if (isAtEnd())
            throw error("unterminated string", "[EOL]");

        advance();

        return token(Token.Type.STRING, line.code.substring(start, index), line.code.substring(start + 1, index - 1));
    }

    private Token integer(int base) {

        int lStart = start;

        if (base != 10){
            advance();
            start = index; //discard the 0x or 0b
        }

        char c;
        while (Character.isDigit(c = peek()) || (base == 16 && (Character.isLetter(c))))
            advance();

        String lexeme = line.code.substring(lStart, index);

        return token(Token.Type.INTEGER, lexeme, Integer.parseInt(line.code.substring(start, index), base));
    }

    private Token macro() {
        String ident = identifier();

        if ("%define".equalsIgnoreCase(ident))
            return token(Token.Type.MACRO_DEFINE);
        if ("%undef".equalsIgnoreCase(ident))
            return token(Token.Type.MACRO_UNDEFINE);

        throw error("invalid macro", ident);
    }

    private Token directive() {
        String ident = identifier();

        if (".org".equalsIgnoreCase(ident))
            return token(Token.Type.DIR_ORG);
        else if (".data".equalsIgnoreCase(ident))
            return token(Token.Type.DIR_DATA);
        throw error("invalid directive", ident);
    }

    private Token token(Token.Type type, String lexeme, Object literal) {
        return new Token(macroSubToken, type, lexeme, literal, line.filename, line.number, start + 1);
    }

    private Token token(Token.Type type, String lexeme) {
        return token(type, lexeme, null);
    }

    private Token token(Token.Type type) {
        return token(type, line.code.substring(start, index), null);
    }

    private String identifier() {
        char c;
        while (Character.isLetterOrDigit(c = peek()) || c == '_')
            advance();
        return line.code.substring(start, index);
    }

    private boolean isAtEnd() {
        return (index >= line.code.length());
    }

    private char advance() {
        if (isAtEnd())
            return '\0';
        return line.code.charAt(index++);
    }

    private char peek() {
        if (isAtEnd())
            return '\0';
        return line.code.charAt(index);
    }

    private void reset(Line line) {
        this.line = line;
        this.start = 0;
        this.index = 0;
    }
}
