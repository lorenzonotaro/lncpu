package com.lnc.assembler.parser;

import com.lnc.common.frontend.Token;

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
