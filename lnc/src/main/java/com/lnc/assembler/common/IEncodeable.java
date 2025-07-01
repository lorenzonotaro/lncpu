package com.lnc.assembler.common;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.linker.LinkInfo;

import java.io.IOException;

public interface IEncodeable {
    int size(ILabelSectionLocator sectionLocator);

    byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress);
}
