package com.lnasm.compiler.parser;

import com.lnasm.compiler.CompileException;
import com.lnasm.compiler.lexer.Token;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractParser<T> {
    private final List<Token[]> lines;
    private Token[] currentLine;
    protected int index;

    public AbstractParser(List<Token[]> lines) {
        this.lines = lines;
        this.index = 0;
    }

    public final boolean parse(){
        boolean success = true;
        for(var line : this.lines){
            try{
                if (line.length == 0) continue;
                currentLine = line;
                index = 0;
                parseLine();
            }catch(CompileException e){
                e.log();
                success = false;
            }
        }
        endParse();
        return success;
    }

    protected boolean isAtEnd() {
        return index >= currentLine.length;
    }

    protected boolean match(Token.Type type) {
        if (!isAtEnd() && peek().type == type) {
            advance();
            return true;
        }
        return false;
    }

    protected Token consume(String errorMsg, Token.Type... types) {
        if (check(types)) return advance();
        else throw error(peek(), errorMsg);
    }

    static CompileException error(Token token, String errorMsg) {
        return new CompileException(errorMsg, token);
    }

    private boolean check(Token.Type... types) {
        if (isAtEnd()) return false;
        Token.Type peekType = peek().type;
        return Arrays.stream(types).anyMatch(t -> t == peekType);
    }

    protected Token advance() {
        if (isAtEnd())
            throw error(previous(), "unexpected end of line");
        return currentLine[index++];
    }

    protected Token previous() {
        return currentLine[index - 1];
    }

    protected Token peek() {
        if (isAtEnd()) {
            throw error(previous(), "unexpected end of line");
        }
        return currentLine[index];
    }

    protected abstract void parseLine();

    protected abstract void endParse();

    public abstract T getResult();
}
