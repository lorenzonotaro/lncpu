package com.lnc.assembler.common;

import com.lnc.assembler.parser.LnasmParser;
import com.lnc.common.frontend.Token;

import java.io.Serializable;

/**
 * Represents label-related information for tokens with associated names.
 * This record class encapsulates a Token and its corresponding label name.
 */
public record LabelInfo(Token token, String name) implements Serializable {

    /**
     * Extracts the sub-label name from the token's lexeme, or returns the entire lexeme if no sub-label separator is found.
     * */
    public String extractSubLabelName() {
        int subIndex = token.lexeme.indexOf(LnasmParser.SUBLABEL_SEPARATOR);
        return subIndex == -1 ? token.lexeme : token.lexeme.substring(subIndex);
    }
}
