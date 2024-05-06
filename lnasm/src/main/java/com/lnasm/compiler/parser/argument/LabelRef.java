package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.*;
import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;

import java.nio.channels.WritableByteChannel;

public class LabelRef extends Argument {
    public final String labelName;

    public LabelRef(Token token) {
        super(token, Type.LABEL);
        this.labelName = token.lexeme;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        SectionInfo sectionInfo = sectionLocator.getSectionInfo(labelName);
        return sectionInfo.type == SectionType.PAGE0 ? 1 : 2;
    }

    @Override
    public void encode(ILabelResolver labelResolver, WritableByteChannel channel, int instructionAddress) {

    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        SectionInfo sectionInfo = sectionLocator.getSectionInfo(labelName);
        return sectionInfo.type == SectionType.PAGE0 ? "cst" : "dcst";
    }
}
