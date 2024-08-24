package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.LabelSectionInfo;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.common.SectionInfo;

import java.util.HashMap;

public class LinkerLabelSectionLocator extends HashMap<String, LabelSectionInfo> implements ILabelSectionLocator {
    private final LinkerConfig config;

    public LinkerLabelSectionLocator(LinkerConfig config) {
        this.config = config;
    }

    @Override
    public SectionInfo getSectionInfo(Token labelToken) {
        LabelSectionInfo labelSectionInfo = get(labelToken.lexeme);
        if(labelSectionInfo == null){
            var info = config.getSectionInfo(labelToken.lexeme);
            if (info == null)
                throw new CompileException("unresolved label '%s'".formatted(labelToken.lexeme), labelToken);

            return info;
        }
        return labelSectionInfo.sectionInfo();
    }
}
