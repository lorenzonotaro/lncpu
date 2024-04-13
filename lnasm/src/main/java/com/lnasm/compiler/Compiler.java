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
        Parser parser = new Parser(preprocessedLines);
        if(!parser.parse())
            return false;
        Set<Block> blocks = parser.getResult();

        Logger.setProgramState("linker");
        AbstractLinker linker = null;
        if("binary".equals(outputFormat)){
            linker = new BinaryLinker(parser.getLabels());
        }else if("immediate".equals(outputFormat)){
            linker = new ImmediateLinker(parser.getLabels());
        }else{
            Logger.error("invalid output format '" + outputFormat + "'");
            return false;
        }

        this.output = linker.link(blocks);

        return this.output != null;
    }


    public byte[] getOutput() {
        return output;
    }
}
