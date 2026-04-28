package com.lnc.assembler.linker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.lnc.assembler.common.SectionInfo;

/**
 * Represents the configuration for linker sections within the application.
 * The configuration manages sections, ensures unique section names, performs input validation,
 * and provides utility methods for querying and combining linker configurations.
 */
public class LinkerConfig{

    public enum ConflictResolutionMode {
        ERROR,
        KEEP_FIRST,
        KEEP_LAST,
    }

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

    public static LinkerConfig join(ConflictResolutionMode conflictResolutionMode, LinkerConfig... configs){
        Map<String, SectionInfo> sectionMap = new HashMap<>();
        for(LinkerConfig config : configs){
            for(SectionInfo section : config.getSectionInfos()){
                if(sectionMap.containsKey(section.getName())){
                    switch(conflictResolutionMode){
                        case KEEP_FIRST -> {}
                        case KEEP_LAST -> sectionMap.put(section.getName(), section);
                        case ERROR -> throw new IllegalArgumentException("section name must be unique");
                    }
                }else{
                    sectionMap.put(section.getName(), section);
                }
            }
        }
        return new LinkerConfig(sectionMap.values().toArray(SectionInfo[]::new));
    }

    public static LinkerConfig join(LinkerConfig... configs) {
        return join(ConflictResolutionMode.ERROR, configs);
    }

    public boolean hasSection(String sectionName) {
        return sectionMap.containsKey(sectionName);
    }
}
