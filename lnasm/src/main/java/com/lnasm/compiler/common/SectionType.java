package com.lnasm.compiler.common;

public enum SectionType {
    ROM("rom", 0x0, 0x1fff),

    RAM("ram", 0x2000, 0x3fff),

    PAGE0("ram", 0x2000, 0x3fff),;

    private final String destCode;
    private final int start;
    private final int size;

    SectionType(String destCode, int start, int size){
        this.destCode = destCode;
        this.start = start;
        this.size = size;
    }

    public String getDestCode() {
        return destCode;
    }

    public int getStart() {
        return start;
    }

    public int getSize() {
        return size;
    }
}
