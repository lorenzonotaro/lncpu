package com.lnasm.compiler.linker;

import com.lnasm.compiler.linker.ILabelSectionLocator;
import com.lnasm.compiler.common.SectionInfo;

import java.util.HashMap;

public class LinkerLabelSectionLocator extends HashMap<String, SectionInfo> implements ILabelSectionLocator {
    @Override
    public SectionInfo getSectionInfo(String label) {
        return get(label);
    }
}
