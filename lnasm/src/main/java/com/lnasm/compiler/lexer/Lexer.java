package com.lnasm.compiler.lexer;

import com.lnasm.compiler.CompileException;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final Token macroSubToken;
    private List<List<Token>> lines;
    private Line line;
    private int index;
    private int start;

    private Token.Type[] keywordSet;

    private final boolean preprocessorDirectives;

    private final boolean comments;

    public Lexer() {
        this(null, Token.Type.LNASM_KEYWORDSET, true, true);
    }

    public Lexer(Token.Type[] keywordSet) {
        this(null, keywordSet, true, true);
    }

    public Lexer(Token macroSubToken, Token.Type[] keywordSet, boolean preprocessorDirectives, boolean comments) {
        this.macroSubToken = macroSubToken;
        this.keywordSet = keywordSet;
        this.preprocessorDirectives = preprocessorDirectives;
        this.comments = comments;
        lines = new ArrayList<>();
    }

    public boolean parse(List<Line> sourceLines){
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
                if(comments)
                    return null;
                else return token(Token.Type.SEMICOLON);
            case '.':
                return directive();
            case '%':
                if (preprocessorDirectives)
                    return macro();
                else throw error("unexpected character", "%");
            case '[':
                return token(Token.Type.L_SQUARE_BRACKET);
            case ']':
                return token(Token.Type.R_SQUARE_BRACKET);
            case '=':
                return token(Token.Type.EQUALS);
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
        for (Token.Type type : keywordSet) {
            if (type.name().equalsIgnoreCase(ident))
                return token(type);
        }
        return token(Token.Type.IDENTIFIER, ident);
    }

    private CompileException error(String message, String lexeme) {
        return new CompileException(message, lexeme, Location.of(line, start + 1));
    }

    private Token string(char terminator) {
        boolean escaped = false;
        while (!isAtEnd() && !(peek() == terminator && !escaped)){
            escaped = advance() == '\\';
        }

        if (isAtEnd())
            throw error("unterminated string", "[EOL]");

        advance();

        String lexeme = line.code.substring(start, index);
        String val = escapeString(line.code.substring(start + 1, index - 1));

        if(val.length() == 1)
            return token(Token.Type.INTEGER, lexeme, (int) val.charAt(0) & 0xFF);

        return token(Token.Type.STRING, lexeme, val);
    }

    private String escapeString(String str){
        StringBuilder sb = new StringBuilder();

        char[] chars = str.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if(c == '\\'){
                c = chars[++i];
                switch (c) {
                    case 'r' -> sb.append('\r');
                    case 'n' -> sb.append('\n');
                    case 'f' -> sb.append('\f');
                    case 'b' -> sb.append('\b');
                    case 't' -> sb.append('\t');
                    case '\'' -> sb.append('\'');
                    case '\"' -> sb.append('\"');
                    case '\\' -> sb.append('\\');
                    case '0'  -> sb.append('\0');
                    default -> throw error("Invalid escape sequence", "\\" + c);
                }
            }else{
                sb.append(c);
            }
        }

        return sb.toString();
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

        String num = line.code.substring(start, index);

        if(num.length() == 0)
            throw error("invalid integer", lexeme);
        return token(Token.Type.INTEGER, lexeme, Integer.parseInt(num, base));
    }

    private Token macro() {
        String ident = identifier();

        if ("%include".equalsIgnoreCase(ident))
            return token(Token.Type.MACRO_INCLUDE);
        if ("%define".equalsIgnoreCase(ident))
            return token(Token.Type.MACRO_DEFINE);
        if ("%undef".equalsIgnoreCase(ident))
            return token(Token.Type.MACRO_UNDEFINE);
        if ("%ifdef".equalsIgnoreCase(ident))
            return token(Token.Type.MACRO_IFDEF);
        if ("%ifndef".equalsIgnoreCase(ident))
            return token(Token.Type.MACRO_IFNDEF);
        if ("%endif".equalsIgnoreCase(ident))
            return token(Token.Type.MACRO_ENDIF);
        if ("%error".equalsIgnoreCase(ident))
            return token(Token.Type.MACRO_ERROR);

        throw error("invalid macro", ident);
    }

    private Token directive() {
        String ident = identifier();

        if (".org".equalsIgnoreCase(ident))
            return token(Token.Type.DIR_ORG);
        else if (".data".equalsIgnoreCase(ident))
            return token(Token.Type.DIR_DATA);
        else if (".res".equals(ident))
            return token(Token.Type.DIR_RES);
        throw error("invalid directive", ident);
    }

    private Token token(Token.Type type, String lexeme, Object literal) {
        return new Token(macroSubToken, type, lexeme, literal, Location.of(line, start + 1));
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
