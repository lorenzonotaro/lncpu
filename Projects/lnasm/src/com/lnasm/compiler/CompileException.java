package com.lnasm.compiler;

import com.lnasm.Logger;

public class CompileException extends RuntimeException{
    final Token token;
    final String file;
    final String lexeme;
    final int line;
    final int col;

    public CompileException(String message, Token token){
        super(message);
        this.token = token;
        this.file = token.file;
        this.lexeme = token.lexeme;
        this.line = token.line;
        this.col = token.col;
    }

    public CompileException(String message, String lexeme, String file, int line, int col) {
        super(message);
        this.lexeme = lexeme;
        this.file = file;
        this.line = line;
        this.col = col;
        this.token = null;
    }

    public void log() {
        if (this.token != null && this.token.macroSub != null) {
            Logger.error(String.format("in file %s:%d:%d, symbol '%s' (expansion of macro %s at %s:%d:%d): %s",
                    this.token.file, this.token.line, this.token.col, this.token.lexeme,
                    this.token.macroSub.lexeme, this.token.macroSub.file, this.token.macroSub.line, this.token.macroSub.col, this.getMessage()));
        } else {
            Logger.error(String.format("in file %s:%d:%d, symbol '%s': %s", this.file, this.line, this.col, this.lexeme, this.getMessage()));
        }
    }
}
