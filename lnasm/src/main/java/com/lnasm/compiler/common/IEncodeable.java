package com.lnasm.compiler.common;

import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public interface IEncodeable {
    int size(ILabelSectionLocator sectionLocator);

    void encode(ILabelResolver labelResolver, WritableByteChannel channel, int instructionAddress) throws IOException;
}
