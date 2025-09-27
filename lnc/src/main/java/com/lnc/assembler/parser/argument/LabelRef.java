package com.lnc.assembler.parser.argument;

import com.lnc.assembler.common.*;
import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.common.frontend.Token;

/**
 * The {@code LabelRef} class represents a reference to a label in an assembly or linking process.
 * It extends the {@code NumericalArgument} class, providing functionality specific to label-based
 * arguments. A {@code LabelRef} object encapsulates a label token and its associated operations
 * for encoding, size calculation, and value resolution.
 *
 * This class is primarily used to manage label references in assembly language compilation,
 * offering methods to:
 * - Resolve and encode label values into byte arrays.
 * - Determine the size of the encoded representation.
 * - Retrieve immediate encodings.
 * - Compare label references for equality.
 *
 * The logic and behavior for these operations depend on the label's resolution, which involves the
 * associated section and its properties, such as whether it resides on a data page or corresponds
 * to a section name.
 */
public class LabelRef extends NumericalArgument {
    public final Token labelToken;

    public LabelRef(Token token) {
        super(token, Type.LABEL);
        this.labelToken = token;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        SectionResolution resolution = sectionLocator.getSectionInfo(labelToken);
        return resolution.sectionInfo().isDataPage() && !resolution.isSectionName() ? 1 : 2;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {
        LabelResolution resolution = labelResolver.resolve(labelToken);

        var targetLabel = resolution.address();

        if(resolution.sectionInfo().isDataPage() && !resolution.isSectionName()){
            return new byte[] { (byte) (targetLabel & 0xFF) };
        }

        return new byte[] { (byte) ((targetLabel >> 8) & 0xFF), (byte) (targetLabel & 0xFF) };
    }

    @Override
    public String toString() {
        return token.lexeme;
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        var resolution = sectionLocator.getSectionInfo(labelToken);
        return resolution.sectionInfo().isDataPage() && !resolution.isSectionName() ? "cst" : "dcst";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LabelRef other)) return false;
        return labelToken.equals(other.labelToken);
    }

    @Override
    public int value(ILabelResolver labelResolver, int instructionAddress) {
        LabelResolution resolution = labelResolver.resolve(labelToken);
        return resolution.address();
    }
}
