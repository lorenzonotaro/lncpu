package com.lnc.assembler.linker;

import com.lnc.assembler.common.SectionResolution;
import com.lnc.common.frontend.Token;

public interface ILabelSectionLocator {
    public SectionResolution getSectionInfo(Token labelToken);
}
