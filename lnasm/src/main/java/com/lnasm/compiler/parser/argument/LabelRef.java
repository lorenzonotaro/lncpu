package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.*;
import com.lnasm.compiler.linker.AbstractLinker;

import java.nio.channels.WritableByteChannel;

public class LabelRef extends Argument {
    public final String labelName;

    public LabelRef(Token token) {
        super(token, Type.LABEL);
        this.labelName = token.lexeme;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        String sectionName = sectionLocator.getSectionName(labelName);
        SectionInfo sectionInfo = linker.getConfig().getSectionInfo(sectionName);
        return sectionInfo.type == SectionType.PAGE0 ? 1 : 2;
    }

    @Override
    public void encode(ILabelSectionLocator sectionLocator, AbstractLinker linker, WritableByteChannel channel) {

    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator, AbstractLinker linker) {
        String sectionName = sectionLocator.getSectionName(labelName);
        SectionInfo sectionInfo = linker.getConfig().getSectionInfo(sectionName);
        return sectionInfo.type == SectionType.PAGE0 ? "cst" : "dcst";
    }
}
