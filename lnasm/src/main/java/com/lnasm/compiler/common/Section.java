package com.lnasm.compiler.common;

import com.lnasm.io.ByteArrayChannel;

public class Section {

    public String name;

    public int startAddress;

    public SectionType type;
    private final ByteArrayChannel channel;

    private Section(String name, int startAddress, SectionType type) {
        this();
        this.name = name;
        this.startAddress = startAddress;
        this.type = type;
    }

    private Section() {
        this.channel = new ByteArrayChannel(0, false);
    }

    public static Section create(String name, int startAddress, SectionType type) {
        return new Section(name, startAddress, type);
    }

    public static Section create(SectionInfo sectionInfo) {
        return new Section(sectionInfo.getName(), sectionInfo.getStart(), sectionInfo.getType());
    }
}
