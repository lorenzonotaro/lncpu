package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.Section;
import com.lnasm.compiler.common.SectionInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LinkerConfig{

    private final SectionInfo[] sections;
    private Map<String, SectionInfo> sectionMap;

    public LinkerConfig(SectionInfo[] sections) {
        this.sections = sections;
        sectionMap = new HashMap<>();
        for (SectionInfo section : sections) {
            if (section == null) {
                throw new IllegalArgumentException("section cannot be null");
            }

            if (sectionMap.containsKey(section.name)) {
                throw new IllegalArgumentException("section name must be unique");
            }

            sectionMap.put(section.name, section);
        }
    }

    @Override
    public String toString() {
        return "LinkerConfig{" +
                "sections=" + Arrays.toString(sections) +
                '}';
    }

    public SectionInfo getSectionInfo(String name) {
        return sectionMap.get(name);
    }
}
