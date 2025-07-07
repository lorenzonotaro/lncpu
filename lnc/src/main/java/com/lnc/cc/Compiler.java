package com.lnc.cc;

import com.lnc.LNC;
import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.assembler.parser.CodeElement;
import com.lnc.cc.anaylsis.Analyzer;
import com.lnc.cc.ast.AST;
import com.lnc.cc.codegen.AsmLevelOptimizer;
import com.lnc.cc.codegen.CodeGenerator;
import com.lnc.cc.codegen.CompilerOutput;
import com.lnc.cc.codegen.GraphColoringRegisterAllocator;
import com.lnc.cc.ir.IR;
import com.lnc.cc.ir.IRGenerator;
import com.lnc.cc.ir.IRLoweringPass;
import com.lnc.cc.ir.IRPrinter;
import com.lnc.cc.optimization.StageOneIROptimizer;
import com.lnc.cc.parser.LncParser;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.Logger;
import com.lnc.common.frontend.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Compiler {
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

        Logger.setProgramState("opt");
        StageOneIROptimizer optimizer = new StageOneIROptimizer();

        for (var unit : irGenerator.getResult().units()) {
            optimizer.run(unit);
        }

        Logger.setProgramState("lowering");
        IRLoweringPass loweringPass = new IRLoweringPass();

        for (var unit : irGenerator.getResult().units()) {
            loweringPass.visit(unit);
        }

        if(!LNC.settings.get("-oM", String.class).isBlank()){
            var irPrinter = new IRPrinter();

            for (var unit : irGenerator.getResult().units()) {
                irPrinter.visit(unit);
            }

            try{
                Files.writeString(Path.of(LNC.settings.get("-oM", String.class)), irPrinter.getResult());
            } catch (IOException e) {
                Logger.error("unable to write IR to file %s: %s".formatted(LNC.settings.get("-oM", String.class), e.getMessage()));
                return false;
            }
        }


        Logger.setProgramState("codegen");
        CodeGenerator codeGenerator = new CodeGenerator(irGenerator.getResult());

        this.output = codeGenerator.run();

        Logger.setProgramState("asmopt");
        AsmLevelOptimizer asmOptimizer = new AsmLevelOptimizer();

        for (var output : this.output) {
            asmOptimizer.optimize(output);
        }

        if(LNC.settings.get("--standalone", Boolean.class)){
            return standalone(irGenerator.getResult());
        }

/*        Logger.setProgramState("optimization");
        LinearOptimizer opt = new LinearOptimizer(irGenerator.getResult());
        var optimizationResult = opt.linearizeAndOptimize();

        Logger.setProgramState("codegen");
        CodeGenerator codeGenerator = new CodeGenerator(optimizationResult);

        codeGenerator.generate();

        this.output = codeGenerator.getOutput();*/

/*        if(LNC.settings.get("--standalone", Boolean.class)){
            return standalone(optimizationResult);
        }*/

        return true;

    }

    private boolean standalone(IR ir) {
        if(ir.units().stream().anyMatch(unit -> {
            var funDecl = unit.getFunctionDeclaration();
            return funDecl.name.lexeme.equals("main") && funDecl.declarator.typeSpecifier().type == TypeSpecifier.Type.VOID && funDecl.parameters.length == 0;
        })){
            this.output.add(0, CodeSnippets.STANDALONE_START_CODE_OUTPUT);
            return true;
        }

        Logger.error("standalone mode requires a void main() function with no parameters.");
        return false;
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
