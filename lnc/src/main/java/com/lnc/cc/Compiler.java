package com.lnc.cc;

import com.lnc.LNC;
import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.cc.anaylsis.Analyzer;
import com.lnc.cc.ast.AST;
import com.lnc.cc.codegen.CompilerOutput;
import com.lnc.cc.ir.IR;
import com.lnc.cc.ir.IRGenerator;
import com.lnc.cc.ir.IRPrinter;
import com.lnc.cc.optimization.linear.LinearOptimizer;
import com.lnc.cc.parser.LncParser;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.Logger;
import com.lnc.common.frontend.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Compiler {
    private static final SectionInfo START_SECTIONINFO = new SectionInfo("_START", 0, LinkTarget.ROM, LinkMode.FIXED, false, false, false);
    private final List<Path> sourceFiles;
    private List<CompilerOutput> output;

    public Compiler(List<Path> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public boolean compile() {
        Logger.setProgramState("lexer");

        LexerConfig lncLexerConfig = new LexerConfig(
                TokenType.LNC_KEYWORDSET,
                LexerConfig.PreprocessorConfig.C_STYLE,
                LexerConfig.SingleLineCommentsConfig.C_STYLE,
                LexerConfig.MultiLineCommentsConfig.C_STYLE,
                false,
                true
        );

        FullSourceLexer lexer = new FullSourceLexer(null, lncLexerConfig);

        var sourceTokens = parseSourceFiles(lexer, this.sourceFiles);

        if (sourceTokens == null)
            return false;

        /* TODO: Adapt and add preprocessor */

        Logger.setProgramState("parser");
        LncParser parser = new LncParser(sourceTokens.toArray(new Token[0]));

        if (!parser.parse())
            return false;

        Logger.setProgramState("analyser");

        AST ast = parser.getResult();

        Analyzer analyzer = new Analyzer(ast);

        if(!analyzer.analyze()){
            return false;
        }

        Logger.setProgramState("irgen");

        IRGenerator irGenerator = new IRGenerator(ast);

        if(!irGenerator.visit()){
            return false;
        }

        if(!LNC.settings.get("-oM", String.class).isBlank()){
            var irPrinter = new IRPrinter();

            var str = irPrinter.print(irGenerator.getResult().units());

            try{
                Files.writeString(Path.of(LNC.settings.get("-oM", String.class)), str);
            } catch (IOException e) {
                Logger.error("unable to write IR to file %s: %s".formatted(LNC.settings.get("-oM", String.class), e.getMessage()));
                return false;
            }
        }

        Logger.setProgramState("optimization");
        LinearOptimizer opt = new LinearOptimizer(irGenerator.getResult());
        var optimizationResult = opt.linearizeAndOptimize();

/*        Logger.setProgramState("codegen");
        CodeGenerator codeGenerator = new CodeGenerator(optimizationResult);

        codeGenerator.generate();

        this.output = codeGenerator.getOutput();*/

        output = new ArrayList<>(); // While we don't have a code generator, we still need to return something

        if(LNC.settings.get("--standalone", Boolean.class)){
            return standalone(optimizationResult);
        }

        return true;

    }

    private boolean standalone(IR ir) {
        if(ir.units().stream().anyMatch(unit -> {
            var funDecl = unit.getFunctionDeclaration();
            return funDecl.name.lexeme.equals("main") && funDecl.declarator.typeSpecifier().type == TypeSpecifier.Type.VOID && funDecl.parameters.length == 0;
        })){
            this.output.add(0, new CompilerOutput(getStartCode(), START_SECTIONINFO));
            return true;
        }

        Logger.error("standalone mode requires a void main() function with no parameters.");
        return false;
    }

    private String getStartCode() {
        try(var stream = LNC.class.getClassLoader().getResourceAsStream("standalone_start.lnasm")){
            if(stream == null){
                Logger.error("unable to load standalone start code.");
                return null;
            }

            return new String(stream.readAllBytes());

        } catch (IOException e) {
            Logger.error("unable to load standalone start code.");
            return null;
        }
    }

    private List<Token> parseSourceFiles(FullSourceLexer lexer, List<Path> sourceFiles) {
        boolean success = true;
        for (Path sourceFile : sourceFiles) {
            try {
                String source = Files.readString(sourceFile);
                if (!lexer.parse(source, sourceFile))
                    success = false;
            } catch (IOException e) {
                Logger.error("unable to open source file %s".formatted(sourceFile.toString()));
                success = false;
            }
        }
        return success ? lexer.getResult() : null;
    }

    public List<CompilerOutput> getOutput() {
        return output;
    }
}
