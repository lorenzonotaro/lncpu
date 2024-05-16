package com.lnasm.compiler.common;

import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;

import java.io.IOException;

public interface IEncodeable {
    int size(ILabelSectionLocator sectionLocator);

    byte[] encode(ILabelResolver labelResolver, int instructionAddress) throws IOException;
}
