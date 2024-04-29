package com.lnasm.compiler.parser.ast;

import com.lnasm.compiler.common.IEncodeable;
import com.lnasm.compiler.common.ILabelResolver;
import com.lnasm.compiler.common.ILabelSectionLocator;

import java.nio.channels.WritableByteChannel;

public class Instruction implements IEncodeable {
    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return 0;
    }

    @Override
    public void encode(ILabelResolver labelResolver, WritableByteChannel channel) {

    }
}
