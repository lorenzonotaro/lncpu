package com.lnc.assembler.linker;

import com.lnc.assembler.common.*;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

import java.util.HashMap;

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
