package com.lnasm.compiler;

import com.lnasm.Logger;
import com.lnasm.compiler.common.SectionType;
import com.lnasm.compiler.lexer.Lexer;
import com.lnasm.compiler.common.Line;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.linker.*;
import com.lnasm.compiler.parser.LnasmParser;
import com.lnasm.compiler.parser.ParseResult;
import com.lnasm.io.ByteArrayChannel;

import java.util.*;

public class Compiler {
    private final List<Line> sourceLines;
    private final List<Line> linkerConfigLines;
    private final String outputFormat;
    private byte[] output;

    public Compiler(List<Line> sourceLines, List<Line> linkerConfigLines, String outputFormat) {
        this.sourceLines = sourceLines;
        this.linkerConfigLines = linkerConfigLines;
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
        LnasmParser parser = new LnasmParser(preprocessedLines);
        if(!parser.parse())
            return false;

        ParseResult parseResult = parser.getResult();

        Logger.setProgramState("linker-config");
        Lexer linkerConfigLexer = new Lexer(null, Token.Type.LINKER_CONFIG_KEYWORDSET, false, false);
        if(!linkerConfigLexer.parse(linkerConfigLines))
            return false;
        LinkerConfigParser linkerconfigParser = new LinkerConfigParser(linkerConfigLexer.getLines().stream().map(l -> l.toArray(new Token[0])).toList());
        if(!linkerconfigParser.parse())
            return false;
        LinkerConfig linkerConfig = linkerconfigParser.getResult();

        Logger.setProgramState("linker");

        BinaryLinker linker = new BinaryLinker(linkerConfig);

        if(!linker.link(parseResult))
            return false;
        else{
            this.output = linker.getResult().getOrDefault(LinkerTarget.ROM, new ByteArrayChannel(0, false)).toByteArray();
        }

        if(this.outputFormat.equals("immediate")){
            Logger.setProgramState("disassembler");
            Disassembler disassembler = new Disassembler(linker.createReverseSymbolTable(), linker.createSectionDescriptors(LinkerTarget.ROM));
            if(disassembler.disassemble(this.output)){
                this.output = disassembler.getOutput();
            }else{
                return false;
            }
        }

        return this.output != null;
    }


    public byte[] getOutput() {
        return output;
    }
}
