package com.lnc.common.frontend;

import java.io.Serializable;

public class Location extends Line implements Serializable {

    public int colNumber;


    Location(Line line, int colNumber) {
        super(line.filepath, line.filename, line.code, line.lineNumber);
        this.colNumber = colNumber;
    }

    public Location(String file, String filename, String code, int line, int col) {
        super(file, filename, code, line);
        this.colNumber = col;
    }

    public static Location of(Line line, int col){
        return new Location(line, col);
    }
}
