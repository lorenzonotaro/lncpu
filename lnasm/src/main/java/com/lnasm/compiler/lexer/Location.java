package com.lnasm.compiler.lexer;

public class Location extends Line{

    public int colNumber;


    Location(Line line, int colNumber) {
        super(line.filepath, line.filename, line.code, line.lineNumber);
        this.colNumber = colNumber;
    }

    public Location(String file, String filename, String lexeme, int line, int col) {
        super(file, filename, lexeme, line);
        this.colNumber = col;
    }

    public static Location of(Line line, int col){
        return new Location(line, col);
    }
}
