package com.lnc.assembler.linker;

import com.lnc.assembler.common.*;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

import java.util.HashMap;

/**
 * This class is responsible for locating and resolving section information for labels during the
 * linking process. It extends {@code HashMap<String, LabelSectionInfo>} to efficiently store
 * mappings between label names and their associated section information, and it implements
 * the {@link ILabelSectionLocator} interface to provide label-to-section resolution functionality.
 *
 * This class uses a combination of direct mappings and a fallback mechanism through the provided
 * {@link LinkerConfig} to resolve sections associated with specific labels. If a label cannot
 * be directly found in the map, the {@link LinkerConfig} is consulted to attempt resolution.
 *
 * An unresolved label results in a {@link CompileException} being thrown.
 */
public class LinkerLabelSectionLocator extends HashMap<String, LabelSectionInfo> implements ILabelSectionLocator {
    private final LinkerConfig config;

    public LinkerLabelSectionLocator(LinkerConfig config) {
        this.config = config;
    }

    @Override
    public SectionResolution getSectionInfo(Token labelToken) {
        LabelSectionInfo labelSectionInfo = get(labelToken.lexeme);
        if(labelSectionInfo == null){
            var info = config.getSectionInfo(labelToken.lexeme);
            if (info == null)
                throw new CompileException("unresolved label '%s'".formatted(labelToken.lexeme), labelToken);
            return new SectionResolution(info, true);
        }
        return new SectionResolution(labelSectionInfo.sectionInfo(), false);
    }
}
