package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.LabelResolution;
import com.lnasm.compiler.common.Token;

public interface ILabelResolver extends ILabelSectionLocator {
    LabelResolution resolve(Token labelToken);
}
