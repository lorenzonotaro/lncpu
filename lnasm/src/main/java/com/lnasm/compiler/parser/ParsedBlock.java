package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.Token;

public class ParsedBlock{
    public final String sectionName;
    public final CodeElement[] instructions;
    public final Token sectionToken;

    public ParsedBlock(Token sectionToken, CodeElement[] instructions) {
        this.sectionToken = sectionToken;
        this.sectionName = sectionToken.lexeme;
        this.instructions = instructions;
    }
}
