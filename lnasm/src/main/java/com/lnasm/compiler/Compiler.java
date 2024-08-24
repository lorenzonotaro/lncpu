package com.lnasm.compiler;

import com.lnasm.LNASM;
import com.lnasm.Logger;
import com.lnasm.compiler.lexer.Lexer;
import com.lnasm.compiler.common.Line;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.linker.*;
import com.lnasm.compiler.parser.LnasmParser;
import com.lnasm.compiler.parser.ParseResult;
import com.lnasm.io.ByteArrayChannel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Compiler {
    private final List<Line> sourceLines;
    private final List<Line> linkerConfigLines;
    private final Map<String, byte[]> outputs;

    public Compiler(List<Line> sourceLines, List<Line> linkerConfigLines) {
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
        if(!"".equals(binOutputFile = LNASM.settings.get("-oB", String.class))){
            this.outputs.put(binOutputFile, binaryResult);
        }

        if(!"".equals(immediateOutputFile = LNASM.settings.get("-oI", String.class))){
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
