package com.lnc.assembler;

import com.lnc.LNC;
import com.lnc.common.Logger;
import com.lnc.common.frontend.Lexer;
import com.lnc.common.frontend.Line;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.linker.*;
import com.lnc.assembler.parser.LnasmParser;
import com.lnc.assembler.parser.ParseResult;
import com.lnc.common.Preprocessor;
import com.lnc.common.io.ByteArrayChannel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Assembler {
    private final List<Line> sourceLines;
    private final List<Line> linkerConfigLines;
    private final Map<String, byte[]> outputs;

    public Assembler(List<Line> sourceLines, List<Line> linkerConfigLines) {
        this.sourceLines = sourceLines;
        this.linkerConfigLines = linkerConfigLines;
        this.outputs = new HashMap<>();
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

        var linkResult = linker.getResult();
        var binaryResult = linkResult.getOrDefault(LinkTarget.ROM, new ByteArrayChannel(0, false)).toByteArray();
        var binOutputFile = "";
        var immediateOutputFile = "";
        if(!"".equals(binOutputFile = LNC.settings.get("-oB", String.class))){
            this.outputs.put(binOutputFile, binaryResult);
        }

        if(!"".equals(immediateOutputFile = LNC.settings.get("-oI", String.class))){
            Logger.setProgramState("disassembler");
            Disassembler disassembler = new Disassembler(linker.createReverseSymbolTable(), linker.createSectionDescriptors(LinkTarget.ROM));
            if(disassembler.disassemble(binaryResult)){
                this.outputs.put(immediateOutputFile, disassembler.getOutput());
            }else{
                return false;
            }
        }

        return this.outputs != null;
    }


    public Map<String, byte[]> getOutputs() {
        return outputs;
    }

    public void writeOutputFiles() {
        for (var entry : this.outputs.entrySet()) {
            try {
                Files.write(Path.of(entry.getKey()), entry.getValue());
            } catch (Exception e) {
                Logger.error("unable to write output file (" + e.getMessage() + ")");
            }
        }
    }
}
