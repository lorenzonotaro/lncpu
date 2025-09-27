package com.lnc.assembler.parser;

import com.lnc.cc.codegen.CompilerOutput;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.LinkedList;

/**
 * Represents a parsed block in a compiled LNASM source. A block consists of a section
 * name and a series of code elements (instructions) corresponding to that section.
 * This class provides utilities to construct parsed block instances and interpret
 * compiler output as a parsed block.
 */
public class LnasmParsedBlock {
    public final String sectionName;
    public final LinkedList<CodeElement> instructions;
    public final Token sectionToken;

    public LnasmParsedBlock(Token sectionToken, LinkedList<CodeElement> instructions) {
        this.sectionToken = sectionToken;
        this.sectionName = sectionToken.lexeme;
        this.instructions = instructions;
    }

    public static LnasmParsedBlock fromCompilerOutput(CompilerOutput output) {
        return new LnasmParsedBlock(
                Token.__internal(TokenType.IDENTIFIER, output.sectionInfo().getName()),
                output.code()
        );
    }
}
