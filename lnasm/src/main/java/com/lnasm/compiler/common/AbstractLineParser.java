package com.lnasm.compiler.common;

import com.lnasm.compiler.common.AbstractParser;
import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.lexer.Token;

import java.util.List;

public abstract class AbstractLineParser<T> extends AbstractParser<T> {
    private final List<Token[]> lines;
    private Token[] currentLine;
    protected int index;

    public AbstractLineParser(List<Token[]> lines) {
        this.lines = lines;
        this.index = 0;
    }

    @Override
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

    @Override
    protected boolean isAtEnd() {
        return index >= currentLine.length;
    }

    @Override
    protected Token advance() {
        if (isAtEnd())
            throw error(previous(), "unexpected end of line");
        return currentLine[index++];
    }

    @Override
    protected Token previous() {
        return currentLine[index - 1];
    }

    @Override
    protected Token peek() {
        if (isAtEnd()) {
            throw error(previous(), "unexpected end of line");
        }
        return currentLine[index];
    }

    protected abstract void parseLine();

    protected abstract void endParse();
}
