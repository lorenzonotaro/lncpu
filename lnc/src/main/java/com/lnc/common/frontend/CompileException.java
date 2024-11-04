package com.lnc.common.frontend;

import com.lnc.common.Logger;

public class CompileException extends RuntimeException{
    final Token token;
    final String lexeme;
    final Location location;

    public CompileException(String message, Token token){
        super(message);
        this.token = token;
        this.lexeme = token.lexeme;
        this.location = token.location;
    }

    public CompileException(String message, String lexeme, Location location) {
        super(message);
        this.lexeme = lexeme;
        this.token = null;
        this.location = location;
    }

    public void log() {
        if (this.token != null && this.token.macroSub != null) {
            Logger.error(String.format("in file %s:%d:%d, symbol '%s' (expansion of macro %s at %s:%d:%d): %s",
                    this.token.macroSub.location.filepath, this.token.macroSub.location.lineNumber, this.token.macroSub.location.colNumber, this.token.lexeme,
                    this.token.macroSub.lexeme, this.token.location.filepath, this.token.location.lineNumber, this.token.location.colNumber, this.getMessage()));
        } else {
            Logger.error(String.format("in file %s:%d:%d, symbol '%s': %s", this.location.filepath, this.location.lineNumber, this.location.colNumber, this.lexeme, this.getMessage()));
        }
    }
}
