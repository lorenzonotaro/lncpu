package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.AbstractLineParser;
import com.lnasm.compiler.common.Token;

import java.util.List;

public class LnasmParser extends AbstractLineParser<ParseResult> {
    public LnasmParser(List<Token[]> preprocessedLines) {
        super(preprocessedLines);
    }

    @Override
    protected void parseLine() {

    }

    @Override
    protected void endParse() {

    }

    @Override
    public ParseResult getResult() {
        return null;
    }
}
