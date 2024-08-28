package com.lnc.common.frontend;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FullSourceLexer extends AbstractLexer<List<Token>>{
    private List<Token> tokens;
    private Line currentLine;
    private Path file;
    private int lastLf;
    private int line;

    public FullSourceLexer(Token macroSubToken, LexerConfig config) {
        super(macroSubToken, config);
        this.tokens = new ArrayList<>();
    }

    @Override
    public Location getLocation() {
        if(file == null){
            return new Location("<>", "<>", source, line, start - lastLf);
        }
        return new Location(file.toAbsolutePath().toString(), file.getFileName().toString(), source.substring(lastLf - 1, index), line, index - lastLf + 1);
    }

    @Override
    public boolean parse(String source, Path file) {
        reset(source);
        this.file = file;

        boolean success = true;

        Token t;
        while (!isAtEnd()) {
            try{
                if((t = next()) != null)
                    tokens.add(t);
            }catch(CompileException e){
                e.log();
                success = false;
            }
        }

        return success;
    }


    @Override
    protected Token match(char c) {
        return super.match(c);
    }

    @Override
    protected void reset(String source) {
        this.line = 1;
        super.reset(source);
    }

    @Override
    public List<Token> getResult() {
        return tokens;
    }

    @Override
    protected char advance() {
        char c = super.advance();
        if(c == '\n'){
            lastLf = index - 1;
            line++;
        }
        return c;
    }
}
