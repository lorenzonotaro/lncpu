package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.SectionInfo;

public interface ILabelSectionLocator {
    public SectionInfo getSectionInfo(String label);
}
