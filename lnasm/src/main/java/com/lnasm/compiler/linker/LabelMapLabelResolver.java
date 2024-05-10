package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.SectionInfo;

import java.util.Map;

public class LabelMapLabelResolver implements ILabelResolver {
    private final LinkerLabelSectionLocator labelLocator;
    private final Map<String, LabelMapEntry> globalLabelMap;

    public LabelMapLabelResolver(LinkerLabelSectionLocator labelLocator, Map<String, LabelMapEntry> globalLabelMap) {
        this.labelLocator = labelLocator;
        this.globalLabelMap = globalLabelMap;

    }

    @Override
    public int resolve(String label) {
        return globalLabelMap.get();
    }

    @Override
    public SectionInfo getSectionInfo(String sectionName) {
        return null;
    }
}
