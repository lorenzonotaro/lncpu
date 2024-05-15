package com.lnasm.compiler.common;

public class SectionInfo {
    private static final int PAGE_0_START = 0x2000;

    public final String name;
    public final int start;
    public final int maxSize;

    public final SectionType type;

    public SectionInfo(String name, int start, SectionType type) {
        this.name = name;
        this.start = start;
        this.type = type;

        if(type == null){
            throw new IllegalArgumentException("null section type");
        }

        if (type == SectionType.PAGE0) {
            if (start != -1) {
                throw new IllegalArgumentException("page0 section cannot have a start address");
            }

            start = PAGE_0_START;
            maxSize = 0xff;
        }else {
            if (start == -1) {
                throw new IllegalArgumentException("non-page0 section must have a start address");
            }
            maxSize = 0x1fff;
        }

    }

    public String toString() {
        return "SectionInfo{" +
                "name='" + name + '\'' +
                ", start=" + start +
                ", type=" + type +
                '}';
    }
}
