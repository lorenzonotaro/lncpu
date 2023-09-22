package com.lnasm.compiler;
public class Line {
    int number;
    String code;
    String filename;

    public Line(int number, String code, String filename) {
        this.number = number;
        this.code = code;
        this.filename = filename;
    }
}
