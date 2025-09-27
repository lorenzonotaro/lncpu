package com.lnc.common.frontend;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The LineByLineLexer class extends the AbstractLexer and is responsible for tokenizing
 * source input line-by-line into a structure that can be used by downstream processes.
 *
 * This lexer processes each line of the input individually, handling compilation errors
 * for individual lines and allowing partial success in the event of errors. It accumulates
 * the tokens for each input line into a nested list structure.
 */
public class LineByLineLexer extends AbstractLexer<List<List<Token>>> {


    private final List<List<Token>> lines;
    private Line currentLine;

    public LineByLineLexer(Token macroSubToken, LexerConfig config) {
        super(macroSubToken, config);
        this.lines = new ArrayList<>();
    }

    public boolean parse(Line[] sourceLines){
        boolean success = true;
        for (Line line : sourceLines) {
            try {
                this.currentLine = line;
                List<Token> tokens = this.parseLine(line);
                lines.add(tokens);
            }catch (CompileException e){
                e.log();
                success = false;
            }
        }
        return success;
    }

    private List<Token> parseLine(Line line) {
        reset(line.code);

        List<Token> tokens = new ArrayList<>();

        Token t;
        while (!isAtEnd() && (t = next()) != null) {
            tokens.add(t);
        }

        return tokens;
    }

    @Override
    public Location getLocation() {
        return new Location(currentLine, start + 1);
    }

    @Override
    public boolean parse(String source, Path file) {
        Line[] lines = Line.fromSource(source, file);
        return parse(lines);
    }

    @Override
    public List<List<Token>> getResult() {
        return lines;
    }
}
