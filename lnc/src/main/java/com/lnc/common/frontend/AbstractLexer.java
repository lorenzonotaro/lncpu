package com.lnc.common.frontend;

import java.nio.file.Path;

/**
 * AbstractLexer is an abstract class that provides a base implementation
 * for lexers capable of tokenizing a given source. This class processes
 * and categorizes input characters into meaningful tokens to be further utilized in
 * compilers or interpreters.
 *
 * @param <T> The type parameter representing customization for token processing.
 */
public abstract class AbstractLexer<T>
{
    protected String source;
    protected int start = 0;
    protected int index = 0;

    protected final Token macroSubToken;
    protected final LexerConfig config;

    public AbstractLexer(Token macroSubToken, LexerConfig config) {
        this.macroSubToken = macroSubToken;
        this.config = config;
    }

    private Token token(TokenType type, String lexeme, Object literal) {
        return new Token(macroSubToken, type, lexeme, literal, getLocation());
    }

    private Token token(TokenType type, String lexeme) {
        return token(type, lexeme, null);
    }

    private Token token(TokenType type) {
        return token(type, source.substring(start, index), null);
    }

    protected final Token next() {
        Token t = match(advance());
        start = index;
        return t;
    }

    protected Token match(char c) {
        switch (c) {
            case '\n':
            case '\t':
            case ' ':
            case '\r':
            case '\b':
                start = index;
                return match(advance());
            case '\0':
                return null;
            case ';':
                if(config.singleLineCommentsConfig() == LexerConfig.SingleLineCommentsConfig.ASM_STYLE) {
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                    return null;
                }
                else return token(TokenType.SEMICOLON);
            case '.':
                if(config.directivesEnabled())
                    return directive();
                return token(TokenType.DOT);
            case '[':
                return token(TokenType.L_SQUARE_BRACKET);
            case ']':
                return token(TokenType.R_SQUARE_BRACKET);
            case '{':
                return token(TokenType.L_CURLY_BRACE);
            case '}':
                return token(TokenType.R_CURLY_BRACE);
            case '=':
                if (peek() == '=') {
                    advance();
                    return token(TokenType.DOUBLE_EQUALS);
                }
                return token(TokenType.EQUALS);
            case ',':
                return token(TokenType.COMMA);
            case ':':
                if(peek() == ':'){
                    advance();
                    return token(TokenType.DOUBLE_COLON);
                }
                return token(TokenType.COLON);
            case '+':
                if (peek() == '+') {
                    advance();
                    return token(TokenType.DOUBLE_PLUS);
                }
                return token(TokenType.PLUS);
            case '-':
                if(peek() == '>'){
                    advance();
                    return token(TokenType.ARROW);
                }else if (peek() == '-') {
                    advance();
                    return token(TokenType.DOUBLE_MINUS);
                }
                return token(TokenType.MINUS);
            case '*':
                return token(TokenType.STAR);
            case '/':
                if(peek() == '/' && config.singleLineCommentsConfig() == LexerConfig.SingleLineCommentsConfig.C_STYLE){
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                    return null;
                }else if(peek() == '*' && config.multiLineCommentsConfig() == LexerConfig.MultiLineCommentsConfig.C_STYLE){
                    do {
                        advance();
                    } while (!(peek() == '*' && peek(1) == '/') && !isAtEnd());
                    if(isAtEnd())
                        throw error("unterminated comment", "EOF");
                    advance();
                    advance();
                    return null;
                }
                return token(TokenType.SLASH);
            case '<':
                if (peek() == '<') {
                    advance();
                    return token(TokenType.BITWISE_LEFT);
                } else if (peek() == '=') {
                    advance();
                    return token(TokenType.LESS_THAN_OR_EQUAL);
                }
                return token(TokenType.LESS_THAN);
            case '>':
                if (peek() == '>') {
                    advance();
                    return token(TokenType.BITWISE_RIGHT);
                }else if (peek() == '=') {
                    advance();
                    return token(TokenType.GREATER_THAN_OR_EQUAL);
                }
                return token(TokenType.GREATER_THAN);
            case '&':
                if (peek() == '&') {
                    advance();
                    return token(TokenType.LOGICAL_AND);
                }
                return token(TokenType.AMPERSAND);
            case '|':
                if (peek() == '|') {
                    advance();
                    return token(TokenType.LOGICAL_OR);
                }
                return token(TokenType.BITWISE_OR);
            case '^':
                return token(TokenType.BITWISE_XOR);
            case '~':
                return token(TokenType.BITWISE_NOT);
            case '(':
                return token(TokenType.L_PAREN);
            case ')':
                return token(TokenType.R_PAREN);
            case '!':
                if (peek() == '=') {
                    advance();
                    return token(TokenType.NOT_EQUALS);
                }
                return token(TokenType.LOGICAL_NOT);
            case '"':
            case '\'':
                return string(c);
            default:
                if((config.preprocessorConfig() == LexerConfig.PreprocessorConfig.ASM_STYLE && c == '%') ||
                        (config.preprocessorConfig() == LexerConfig.PreprocessorConfig.C_STYLE && c == '#'))
                    return macro();
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
        for (TokenType type : config.keywordSet()) {
            if (stringEquals(type.getStrValue(), ident))
                return token(type);
        }
        return token(TokenType.IDENTIFIER, ident);
    }

    private CompileException error(String message, String lexeme) {
        return new CompileException(message, lexeme, getLocation());
    }

    private Token string(char terminator) {
        boolean escaped = false;
        while (!isAtEnd() && !(peek() == terminator && !escaped)){
            escaped = advance() == '\\';
        }

        if (isAtEnd())
            throw error("unterminated string", "[EOL]");

        advance();

        String lexeme = source.substring(start, index);
        String val = escapeString(source.substring(start + 1, index - 1));

        if(val.length() == 1)
            return token(TokenType.INTEGER, lexeme, (int) val.charAt(0) & 0xFF);

        return token(TokenType.STRING, lexeme, val);
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

        String lexeme = source.substring(lStart, index);

        String num = source.substring(start, index);

        if(num.isEmpty())
            throw error("invalid integer", lexeme);
        return token(TokenType.INTEGER, lexeme, Integer.parseInt(num, base));
    }

    private Token macro() {
        String ident = identifier().substring(1);

        if(stringEquals("include", ident))
            return token(TokenType.MACRO_INCLUDE);
        if(stringEquals("define", ident))
            return token(TokenType.MACRO_DEFINE);
        if(stringEquals("undef", ident))
            return token(TokenType.MACRO_UNDEFINE);
        if(stringEquals("ifdef", ident))
            return token(TokenType.MACRO_IFDEF);
        if(stringEquals("ifndef", ident))
            return token(TokenType.MACRO_IFNDEF);
        if(stringEquals("endif", ident))
            return token(TokenType.MACRO_ENDIF);
        if(stringEquals("error", ident))
            return token(TokenType.MACRO_ERROR);

        throw error("invalid macro", ident);
    }

    private Token directive() {
        String ident = identifier().substring(1);

        if (stringEquals("section", ident))
            return token(TokenType.DIR_SECTION);
        if (stringEquals("data", ident))
            return token(TokenType.DIR_DATA);
        if (stringEquals("res", ident))
            return token(TokenType.DIR_RES);
        if (stringEquals("export", ident))
            return token(TokenType.DIR_EXPORT);

        throw error("invalid directive", ident);
    }

    protected String identifier() {
        char c;
        while (Character.isLetterOrDigit(c = peek()) || c == '_')
            advance();
        return source.substring(start, index);
    }

    protected boolean isAtEnd() {
        return (index >= source.length());
    }

    protected char advance() {
        if (isAtEnd())
            return '\0';
        return source.charAt(index++);
    }

    protected char peek() {
        return peek(0);
    }

    private char peek(int lookAhead) {
        return index + lookAhead >= source.length() ? '\0' : source.charAt(index + lookAhead);
    }

    protected void reset(String source) {
        this.source = source;
        this.start = 0;
        this.index = 0;
    }

    private boolean stringEquals(String a, String b) {
        return a.equals(b) || (!config.caseSensitive() && a.equalsIgnoreCase(b));
    }

    public abstract Location getLocation();

    protected abstract boolean parse(String source, Path file);

    public abstract T getResult();
}
