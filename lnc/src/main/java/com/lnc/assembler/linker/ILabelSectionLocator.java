package com.lnc.assembler.linker;

import com.lnc.assembler.common.SectionResolution;
import com.lnc.common.frontend.Token;

/**
 * Interface for resolving the section information associated with a given label token.
 *
 * Implementations of this interface are responsible for determining and returning
 * the section where a label resides, based on the provided label token.
 * The returned section information may also indicate whether the label corresponds
 * to a section name or not.
 *
 * This interface is typically used in assembly or linking processes to resolve the
 * mapping between labels and their associated sections in memory or code structure.
 */
public interface ILabelSectionLocator {
    SectionResolution getSectionInfo(Token labelToken);
}
