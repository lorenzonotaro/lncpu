package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.SectionInfo;
import com.lnasm.compiler.common.Token;

public interface ILabelSectionLocator {
    public SectionInfo getSectionInfo(Token labelToken);
}
