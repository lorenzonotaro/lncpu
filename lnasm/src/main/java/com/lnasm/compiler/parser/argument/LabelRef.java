package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.*;
import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;
import com.lnasm.compiler.linker.LinkInfo;

import java.io.IOException;

public class LabelRef extends Argument {
    public final Token labelToken;

    public LabelRef(Token token) {
        super(token, Type.LABEL, true);
        this.labelToken = token;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        SectionInfo sectionInfo = sectionLocator.getSectionInfo(labelToken);
        return sectionInfo.isDataPage() ? 1 : 2;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) throws IOException {
        int targetLabel = labelResolver.resolve(labelToken);

        if(labelResolver.getSectionInfo(labelToken).isDataPage()){
            return new byte[] { (byte) (targetLabel & 0xFF) };
        }

        return new byte[] { (byte) ((targetLabel >> 8) & 0xFF), (byte) (targetLabel & 0xFF) };
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        SectionInfo sectionInfo = sectionLocator.getSectionInfo(labelToken);
        return sectionInfo.isDataPage() ? "cst" : "dcst";
    }
}
