package com.lnc.assembler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lnc.LNC;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.BinaryLinker;
import com.lnc.assembler.linker.Disassembler;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.assembler.linker.LinkerConfig;
import com.lnc.assembler.linker.LinkerConfigParser;
import com.lnc.assembler.parser.LnasmParseResult;
import com.lnc.assembler.parser.LnasmParsedBlock;
import com.lnc.assembler.parser.LnasmParser;
import com.lnc.cc.codegen.CompilerOutput;
import com.lnc.common.Logger;
import com.lnc.common.Preprocessor;
import com.lnc.common.frontend.FullSourceLexer;
import com.lnc.common.frontend.LexerConfig;
import com.lnc.common.frontend.LineByLineLexer;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;
import com.lnc.common.io.ByteArrayChannel;

public class Assembler {
    private final List<Path> sourceFiles;
    private final String linkerConfig;
    private final Map<String, byte[]> outputs;
    private final List<CompilerOutput> compilerOutputs;

    public Assembler(List<Path> sourceFiles, String linkerConfig) {
        this(sourceFiles, linkerConfig, null);
    }

    public Assembler(List<Path> sourceFiles, String linkerConfig, List<CompilerOutput> compilerOutputs) {
        this.sourceFiles = sourceFiles;
        this.linkerConfig = linkerConfig;

        this.compilerOutputs = compilerOutputs;

        this.outputs = new HashMap<>();
    }

    public boolean assemble() {
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

        LinkerConfigParser linkerconfigParser = new LinkerConfigParser(linkerConfigLexer.getResult().toArray(Token[]::new));
        if(!linkerconfigParser.parse())
            return false;

        LinkerConfig linkerConfig = linkerconfigParser.getResult();

        if(this.compilerOutputs != null){
            try{
                linkerConfig = LinkerConfig.join(linkerConfig, new LinkerConfig(compilerOutputs.stream().map(CompilerOutput::sectionInfo).toArray(SectionInfo[]::new)));
            }catch(IllegalArgumentException e){
                Logger.error("unable to merge the provided linker config with the compiler-generated one: %s.".formatted(e.getMessage()));
                return false;
            }
        }

        Logger.setProgramState("preprocessor");
        Preprocessor preprocessor = new Preprocessor(lines, asmLexerConfig, linkerConfig);
        if(!preprocessor.preprocess())
            return false;
        List<Token[]> preprocessedLines = preprocessor.getLines();

        Logger.setProgramState("parser");
        LnasmParser parser = new LnasmParser(preprocessedLines);
        if(!parser.parse())
            return false;

        LnasmParseResult parseResult = parser.getResult();

        if(compilerOutputs != null){
            parseResult.join(compilerOutputs.stream().map(LnasmParsedBlock::fromCompilerOutput).toList());
        }

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
