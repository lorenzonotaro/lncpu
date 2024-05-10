package com.lnasm.compiler.linker;

public interface ILabelResolver extends ILabelSectionLocator {
    int resolve(String label);
}
