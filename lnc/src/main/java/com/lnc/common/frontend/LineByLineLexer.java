package com.lnc.common.frontend;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LineByLineLexer extends AbstractLexer<List<List<Token>>> {


    private List<List<Token>> lines;
    private Line currentLine;

    public LineByLineLexer(Token macroSubToken, LexerConfig config) {
        super(macroSubToken, config);
        this.lines = new ArrayList<>();
    }

    public boolean parse(Line[] sourceLines){
        lines.clear();
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
