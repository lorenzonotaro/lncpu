package com.lnasm.compiler.common;

public class SectionInfo {
    private static final int PAGE_0_START = 0x2000;

    private String name;
    private int start;
    private int maxSize;

    private SectionType type;

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

            this.start = PAGE_0_START;
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
                "name='" + getName() + '\'' +
                ", start=" + getStart() +
                ", type=" + getType() +
                '}';
    }

    public String getName() {
        return name;
    }

    public int getStart() {
        return start;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public SectionType getType() {
        return type;
    }
}
