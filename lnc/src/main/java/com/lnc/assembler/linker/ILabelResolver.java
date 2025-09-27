package com.lnc.assembler.linker;

import com.lnc.assembler.common.LabelResolution;
import com.lnc.common.frontend.Token;

/**
 * Defines an interface for resolving labels in the context of an assembly or linking process.
 * It extends {@code ILabelSectionLocator}, allowing it to provide section resolution
 * information for labels in addition to label-specific resolution.
 *
 * The primary role of an implementation is to map a label token to its resolved details,
 * which include its location within a specific section, the associated memory address,
 * and whether or not the label corresponds to a section name.
 *
 * Responsibilities:
 * - Resolve labels using provided tokens.
 * - Provide information about the section where the label resides.
 *
 * The interface is expected to be implemented in systems dealing with assembly or linking mechanics.
 */
public interface ILabelResolver extends ILabelSectionLocator {
    /**
     * Resolves a label token to its corresponding label resolution details.
     *
     * @param labelToken the token representing the label to be resolved.
     * @return a {@code LabelResolution} object containing the resolved section information,
     *         address, and whether it is a section name.
     */
    LabelResolution resolve(Token labelToken);
}
