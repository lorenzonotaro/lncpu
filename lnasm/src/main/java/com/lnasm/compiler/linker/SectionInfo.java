package com.lnasm.compiler.linker;

public class SectionInfo {
    protected final String name;
    protected final int start;
    protected final SectionType type;

    public SectionInfo(String name, int start, SectionType type) {
        this.name = name;
        this.start = start;
        this.type = type;

        if(type == null){
            throw new IllegalArgumentException("null section type");
        }

        if(type == SectionType.PAGE0 && start != -1){
            throw new IllegalArgumentException("page0 section cannot have a start address");
        }

        if(type != SectionType.PAGE0 && start == -1){
            throw new IllegalArgumentException("non-page0 section must have a start address");
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
