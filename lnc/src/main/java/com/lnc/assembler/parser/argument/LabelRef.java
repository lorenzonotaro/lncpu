package com.lnc.assembler.parser.argument;

import com.lnc.assembler.common.*;
import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.linker.LinkInfo;
import com.lnc.common.frontend.Token;

import java.io.IOException;

public class LabelRef extends Argument {
    public final Token labelToken;

    public LabelRef(Token token) {
        super(token, Type.LABEL, true);
        this.labelToken = token;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        SectionResolution resolution = sectionLocator.getSectionInfo(labelToken);
        return resolution.sectionInfo().isDataPage() && !resolution.isSectionName() ? 1 : 2;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) throws IOException {
        LabelResolution resolution = labelResolver.resolve(labelToken);

        var targetLabel = resolution.address();

        if(resolution.sectionInfo().isDataPage() && !resolution.isSectionName()){
            return new byte[] { (byte) (targetLabel & 0xFF) };
        }

        return new byte[] { (byte) ((targetLabel >> 8) & 0xFF), (byte) (targetLabel & 0xFF) };
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        var resolution = sectionLocator.getSectionInfo(labelToken);
        return resolution.sectionInfo().isDataPage() && !resolution.isSectionName() ? "cst" : "dcst";
    }
}
