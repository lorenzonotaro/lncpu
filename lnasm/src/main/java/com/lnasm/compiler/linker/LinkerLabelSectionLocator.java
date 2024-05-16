package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.LabelSectionInfo;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.common.SectionInfo;

import java.util.HashMap;

public class LinkerLabelSectionLocator extends HashMap<String, LabelSectionInfo> implements ILabelSectionLocator {
    @Override
    public SectionInfo getSectionInfo(Token labelToken) {
        LabelSectionInfo labelSectionInfo = get(labelToken.lexeme);
        if(labelSectionInfo == null){
            throw new CompileException("unresolved label '%s'".formatted(labelToken.lexeme), labelToken);
        }
        return labelSectionInfo.sectionInfo();
    }
}
