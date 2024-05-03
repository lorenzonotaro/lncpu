package com.lnasm.compiler.common;

import com.lnasm.compiler.linker.AbstractLinker;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public interface IEncodeable {
    int size(ILabelSectionLocator sectionLocator, AbstractLinker linker);

    void encode(ILabelSectionLocator sectionLocator, AbstractLinker linker, WritableByteChannel channel) throws IOException;
}
