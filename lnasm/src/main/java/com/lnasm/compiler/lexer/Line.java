package com.lnasm.compiler.lexer;

import java.nio.file.Path;

public class Line {
    public int lineNumber;
    public String code;
    public String filename;
    public String filepath;

    public Line(String filepath, String filename, String code, int lineNumber) {
        this.lineNumber = lineNumber;
        this.code = code;
        this.filename = filename;
        this.filepath = filepath;
    }

    public Line(Path path, String code, int lineNumber) {
        this.lineNumber = lineNumber;
        this.code = code;
        this.filepath = path.toAbsolutePath().toString();
        this.filename = path.getFileName().toString();
    }

}
