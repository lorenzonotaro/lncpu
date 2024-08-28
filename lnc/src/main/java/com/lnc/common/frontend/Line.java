package com.lnc.common.frontend;

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

    public static Line[] fromSource(String source, Path file) {
        String[] lines = source.split("\n");
        Line[] result = new Line[lines.length];
        for (int i = 0; i < lines.length; i++) {
            result[i] = new Line(file, lines[i], i + 1);
        }
        return result;
    }
}
