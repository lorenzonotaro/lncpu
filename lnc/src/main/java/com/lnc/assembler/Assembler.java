package com.lnc.assembler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lnc.LNC;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.*;
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
    private final Map<LinkTarget, byte[]> outputs;
    private final List<CompilerOutput> compilerOutputs;
    private final LinkTarget[] requestedOutputs;
    private BinaryLinker linker;
    private Set<String> exportedLabels;

    public Assembler(List<Path> sourceFiles, String linkerConfig, List<CompilerOutput> compilerOutputs, LinkTarget[] requestedOutputs) {
        this.sourceFiles = sourceFiles;
        this.linkerConfig = linkerConfig;

        this.compilerOutputs = compilerOutputs;
        this.requestedOutputs = requestedOutputs;

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

        if (!linkerConfigLexer.parse(linkerConfig, null))
            return false;

        LinkerConfigParser linkerconfigParser = new LinkerConfigParser(linkerConfigLexer.getResult().toArray(Token[]::new));
        if (!linkerconfigParser.parse())
            return false;

        LinkerConfig linkerConfig = linkerconfigParser.getResult();

        if (this.compilerOutputs != null) {
            try {
                linkerConfig = LinkerConfig.join(linkerConfig, new LinkerConfig(compilerOutputs.stream().map(CompilerOutput::sectionInfo).toArray(SectionInfo[]::new)));
            } catch (IllegalArgumentException e) {
                Logger.error("unable to merge the provided linker config with the compiler-generated one: %s.".formatted(e.getMessage()));
                return false;
            }
        }

        Logger.setProgramState("preprocessor");
        Preprocessor preprocessor = new Preprocessor(lines, asmLexerConfig, linkerConfig);
        if (!preprocessor.preprocess())
            return false;
        List<Token[]> preprocessedLines = preprocessor.getLines();

        Logger.setProgramState("parser");
        LnasmParser parser = new LnasmParser(preprocessedLines);
        if (!parser.parse())
            return false;

        LnasmParseResult parseResult = parser.getResult();

        if (compilerOutputs != null) {
            parseResult.join(compilerOutputs.stream().map(LnasmParsedBlock::fromCompilerOutput).toList(), compilerOutputs.stream().flatMap(c -> c.exportedLabels().stream()).toList());
        }

        this.exportedLabels = parseResult.exportedLabels();

        Logger.setProgramState("linker");

        String symTables = LNC.settings.get("-S", String.class);

        if(symTables.isEmpty()){
            this.linker = new BinaryLinker(linkerConfig);
        }else{
            this.linker = new BinaryLinker(linkerConfig, List.of(symTables.split(";")));
        }


        if (!linker.link(parseResult))
            return false;

        var linkResult = linker.getResult();

        for (var target : this.requestedOutputs) {
            this.outputs.put(target, linkResult.getOrDefault(target, new ByteArrayChannel(0, true)).toByteArray());
        }

        return true;
    }

    public void writeOutputFiles() {
        // binary outputs
        String binaryOutputFile;
        if (!"".equals(binaryOutputFile = LNC.settings.get("-oB", String.class))) {
            for (var entry : this.outputs.entrySet()) {
                try {
                    LinkTarget linkTarget = entry.getKey();
                    String filename = appendTargetToFilename(linkTarget, binaryOutputFile);
                    Files.write(Path.of(filename), entry.getValue());
                } catch (Exception e) {
                    Logger.error("unable to write output file (" + e.getMessage() + ")");
                }
            }
        }
        String immediateOutputFile;

        if (!"".equals(immediateOutputFile = LNC.settings.get("-oI", String.class))) {
            Logger.setProgramState("disassembler");
            Map<Integer, Set<String>> reverseSymbolTable = linker.createReverseSymbolTable();
            for (var entry : this.outputs.entrySet()) {
                try {
                    LinkTarget linkTarget = entry.getKey();
                    List<SectionBuilder.Descriptor> sectionDescriptors = linker.createSectionDescriptors(linkTarget);
                    Disassembler disassembler = new Disassembler(reverseSymbolTable, sectionDescriptors);

                    if (!disassembler.disassemble(entry.getValue())) {
                        Logger.error("unable to disassemble the binary for output: " + immediateOutputFile + ", device target: " + linkTarget.name());
                        return;
                    }

                    String filename = appendTargetToFilename(linkTarget, immediateOutputFile);
                    Files.writeString(Path.of(filename), new String(disassembler.getOutput()));
                } catch (Exception e) {
                    Logger.error("unable to write output file (" + e.getMessage() + ")");
                }
            }
        }
    }

    private static String appendTargetToFilename(LinkTarget linkTarget, String outputFileName) {
        String filename;
        if (linkTarget == LinkTarget.ROM) {
            filename = outputFileName;
        } else {
            int dotIndex = outputFileName.lastIndexOf('.');
            if (dotIndex != -1) {
                filename = outputFileName.substring(0, dotIndex) + "_" + linkTarget.name() + outputFileName.substring(dotIndex);
            } else {
                filename = outputFileName + "_" + linkTarget.name();
            }
        }
        return filename;
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

    public void writeSymTable(String symOut) {
        try {
            ExternalSymbolTableIO.write(symOut, linker.getLabelResolver().getEntriesFor(this.exportedLabels));
        } catch (Exception e) {
            Logger.error("unable to write symbol table to file %s: %s".formatted(symOut, e.getMessage()));
        }
    }
}
