package com.lnc.assembler.linker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.lnc.assembler.common.SectionInfo;

public class LinkerConfig{

    private final SectionInfo[] sections;
    private final Map<String, SectionInfo> sectionMap;

    public LinkerConfig(SectionInfo[] sections) {
        this.sections = sections;
        sectionMap = new HashMap<>();
        for (SectionInfo section : sections) {
            if (section == null) {
                throw new IllegalArgumentException("section cannot be null");
            }

            if (sectionMap.containsKey(section.getName())) {
                throw new IllegalArgumentException("section name must be unique");
            }

            sectionMap.put(section.getName(), section);
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

    public SectionInfo[] getSectionInfos() {
        return sections;
    }

    public static LinkerConfig join(LinkerConfig... configs) {
        SectionInfo[] sections = Arrays.stream(configs)
                .map(LinkerConfig::getSectionInfos)
                .flatMap(Arrays::stream)
                .toArray(SectionInfo[]::new);

        return new LinkerConfig(sections);
    }

    public boolean hasSection(String sectionName) {
        return sectionMap.containsKey(sectionName);
    }
}
