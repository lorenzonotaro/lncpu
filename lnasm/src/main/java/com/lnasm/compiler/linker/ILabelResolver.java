package com.lnasm.compiler.linker;

import com.lnasm.compiler.linker.ILabelSectionLocator;

public interface ILabelResolver extends ILabelSectionLocator {
    short resolve(String label);
}
