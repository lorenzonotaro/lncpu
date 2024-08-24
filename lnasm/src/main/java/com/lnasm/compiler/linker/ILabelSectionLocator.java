package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.SectionResolution;
import com.lnasm.compiler.common.Token;

public interface ILabelSectionLocator {
    public SectionResolution getSectionInfo(Token labelToken);
}
