package com.lnc.cc;

import com.lnc.cc.anaylsis.Analyzer;
import com.lnc.cc.ast.AST;
import com.lnc.cc.ir.IRGenerator;
import com.lnc.cc.parser.LncParser;
import com.lnc.common.Logger;
import com.lnc.common.frontend.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Compiler {
    private final List<Path> sourceFiles;

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

        return irGenerator.visit();

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
}
