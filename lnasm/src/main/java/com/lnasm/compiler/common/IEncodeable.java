package com.lnasm.compiler.common;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public interface IEncodeable {
    int size(ILabelSectionLocator sectionLocator);

    void encode(ILabelResolver labelResolver, WritableByteChannel channel) throws IOException;
}
