package com.lnc.assembler;

import com.lnc.LNC;
import com.lnc.common.Logger;
import com.lnc.common.frontend.*;
import com.lnc.assembler.linker.*;
import com.lnc.assembler.parser.LnasmParser;
import com.lnc.assembler.parser.ParseResult;
import com.lnc.common.Preprocessor;
import com.lnc.common.io.ByteArrayChannel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Assembler {
    private final List<Path> sourceFiles;
    private final String linkerConfig;
    private final Map<String, byte[]> outputs;

    public Assembler(List<Path> sourceFiles, String linkerConfig) {
        this.sourceFiles = sourceFiles;
        this.linkerConfig = linkerConfig;
        this.outputs = new HashMap<>();
    }

    public boolean compile() {
        Logger.setProgramState("lexer");

        LexerConfig asmLexerConfig = new LexerConfig(
                TokenType.LNASM_KEYWORDSET,
                LexerConfig.PreprocessorConfig.ASM_STYLE,
                LexerConfig.SingleLineCommentsConfig.ASM_STYLE,
                LexerConfig.MultiLineCommentsConfig.DISABLED,
                true,
                false
        );
        LineByLineLexer asmLexer = new LineByLineLexer(null, asmLexerConfig);

        var lines = parseSourceFiles(asmLexer, this.sourceFiles);

        if (lines == null)
            return false;

        Logger.setProgramState("preprocessor");
        Preprocessor preprocessor = new Preprocessor(lines, asmLexerConfig);
        if(!preprocessor.preprocess())
            return false;
        List<Token[]> preprocessedLines = preprocessor.getLines();

        Logger.setProgramState("parser");
        LnasmParser parser = new LnasmParser(preprocessedLines);
        if(!parser.parse())
            return false;

        ParseResult parseResult = parser.getResult();

        Logger.setProgramState("linker-config");
        FullSourceLexer linkerConfigLexer = new FullSourceLexer(null, new LexerConfig(
                TokenType.LINKER_CONFIG_KEYWORDSET,
                LexerConfig.PreprocessorConfig.DISABLED,
                LexerConfig.SingleLineCommentsConfig.DISABLED,
                LexerConfig.MultiLineCommentsConfig.DISABLED,
                false,
                false
        ));

        if(!linkerConfigLexer.parse(linkerConfig, null))
            return false;

        LinkerConfigParser linkerconfigParser = new LinkerConfigParser(linkerConfigLexer.getResult().toArray(new Token[0]));
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

        return true;
    }

    private List<List<Token>> parseSourceFiles(LineByLineLexer lexer, List<Path> sourceFiles) {
        boolean success = true;
        for (Path sourceFile : sourceFiles) {
            try {
                String source = Files.readString(sourceFile);
                if (!lexer.parse(source, sourceFile))
                    success = false;
            } catch (Exception e) {
                Logger.error("unable to open source file %s".formatted(sourceFile.toString()));
                success = false;
            }
        }
        return success ? lexer.getResult() : null;
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
