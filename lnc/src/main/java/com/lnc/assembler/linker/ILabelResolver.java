package com.lnc.assembler.linker;

import com.lnc.assembler.common.LabelResolution;
import com.lnc.common.frontend.Token;

public interface ILabelResolver extends ILabelSectionLocator {
    LabelResolution resolve(Token labelToken);
}
