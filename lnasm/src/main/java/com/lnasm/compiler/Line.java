package com.lnasm.compiler;

import java.nio.file.Path;

public class Line {
    int lineNumber;
    String code;
    String filename;
    String filepath;

    protected Line(String filepath, String filename, String code, int lineNumber) {
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
