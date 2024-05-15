package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.Token;

public interface ILabelResolver extends ILabelSectionLocator {
    int resolve(Token labelToken);
}
