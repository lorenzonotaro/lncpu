package com.lnasm.compiler;

import com.lnasm.Logger;

import java.util.*;

public class Compiler {
    private final List<Line> sourceLines;
    private final String outputFormat;
    private byte[] output;

    public Compiler(List<Line> sourceLines, String outputFormat) {
        this.sourceLines = sourceLines;
        this.outputFormat = outputFormat;
    }

    public boolean compile() {
        if(!"binary".equals(outputFormat)){
            Logger.error("invalid output format '" + outputFormat + "'");
            return false;
        }

        Logger.setProgramState("lexer");
        Lexer lexer = new Lexer();
        if(!lexer.parse(this.sourceLines))
            return false;
        List<List<Token>> lines = lexer.getLines();

        Logger.setProgramState("preprocessor");
        Preprocessor preprocessor = new Preprocessor(lines);
        if(!preprocessor.preprocess())
            return false;
        List<Token[]> preprocessedLines = preprocessor.getLines();

        Logger.setProgramState("parser");
        Parser parser = new Parser();
        if(!parser.parse(preprocessedLines))
            return false;
        Set<Segment> segments = parser.getSegments();

        Logger.setProgramState("linker");
        Linker linker = new Linker();
        if(!linker.link(segments))
            return false;

        this.output = linker.getOutput();
        return true;

    }


    public byte[] getOutput() {
        return output;
    }
}
