package com.lnc.assembler.common;

import com.lnc.assembler.parser.LnasmParser;
import com.lnc.common.frontend.Token;

public record LabelInfo(Token token, String name) {

    /**
     * Extracts the sub-label name from the token's lexeme, or returns the entire lexeme if no sub-label separator is found.
     * */
    public String extractSubLabelName() {
        int subIndex = token.lexeme.indexOf(LnasmParser.SUBLABEL_SEPARATOR);
        return subIndex == -1 ? token.lexeme : token.lexeme.substring(subIndex);
    }
}
