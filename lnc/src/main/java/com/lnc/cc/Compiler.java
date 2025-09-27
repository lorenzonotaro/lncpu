package com.lnc.cc;

import com.lnc.LNC;
import com.lnc.cc.anaylsis.ASTAnalyzer;
import com.lnc.cc.ast.AST;
import com.lnc.cc.optimization.asm.AsmLevelOptimizer;
import com.lnc.cc.codegen.CodeGenerator;
import com.lnc.cc.codegen.CompilerOutput;
import com.lnc.cc.ir.*;
import com.lnc.cc.optimization.ir.StageOneIROptimizer;
import com.lnc.cc.parser.LncParser;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.Logger;
import com.lnc.common.Preprocessor;
import com.lnc.common.frontend.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Represents a compiler instance capable of processing a list of source files
 * and performing a full compilation pipeline, including tokenization, parsing,
 * analysis, and code generation.
 *
 * This class provides methods to manage the compilation process and access
 * the resulting compiled outputs.
 */
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

        Preprocessor preprocessor = Preprocessor.lnc(sourceTokens, lncLexerConfig);

        if(!preprocessor.preprocess()){
            return false;
        }

        sourceTokens = preprocessor.getTokens();

        Logger.setProgramState("parser");
        LncParser parser = new LncParser(sourceTokens.toArray(new Token[0]));

        if (!parser.parse())
            return false;

        Logger.setProgramState("analyser");

        AST ast = parser.getResult();

        ASTAnalyzer analyzer = new ASTAnalyzer(ast);

        if(!analyzer.analyze()){
            return false;
        }

        Logger.setProgramState("irgen");

        IRGenerator irGenerator = new IRGenerator(ast);

        if(!irGenerator.visit()){
            return false;
        }

        IR result = irGenerator.getResult();

        Logger.setProgramState("iranalysis");
        IRAnalysisPass irAnalysisPass = new IRAnalysisPass();

        for (var unit : result.units()) {
            irAnalysisPass.visit(unit);
        }


        Logger.setProgramState("lowering");
        IRLoweringPass loweringPass = new IRLoweringPass();

        for (var unit : result.units()) {
            loweringPass.visit(unit);
        }

        if(!LNC.settings.get("--no-ir-opt", Boolean.class)) {
            Logger.setProgramState("opt");
            StageOneIROptimizer optimizer = new StageOneIROptimizer();

            for (var unit : result.units()) {
                optimizer.run(unit);
            }
        }

        if(!LNC.settings.get("-oM", String.class).isBlank()){
            var irPrinter = new IRPrinter();

            for (var unit : result.units()) {
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
        CodeGenerator codeGenerator = new CodeGenerator(result);

        this.output = codeGenerator.run();

        Logger.setProgramState("asmopt");
        AsmLevelOptimizer asmOptimizer = new AsmLevelOptimizer();

        Boolean noAsmLevelOpts = LNC.settings.get("--no-asm-opt", Boolean.class);

        for (var output : this.output) {
            if(!noAsmLevelOpts){
                asmOptimizer.optimize(output);
            }
            if(output.unit() != null){
                asmOptimizer.stackFramePreservation(output);
                output.addUnitLabel();
            }
        }

        if(LNC.settings.get("--standalone", Boolean.class)){
            return standalone(result);
        }

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
