package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.SectionInfo;
import com.lnasm.compiler.common.Token;

import java.util.Map;

public class LabelMapLabelResolver implements ILabelResolver {
    private final Map<String, LabelMapEntry> globalLabelMap;

    public LabelMapLabelResolver(Map<String, LabelMapEntry> globalLabelMap) {
        this.globalLabelMap = globalLabelMap;
    }

    @Override
    public int resolve(Token labelToken) {
        LabelMapEntry entry = globalLabelMap.get(labelToken.lexeme);
        if(entry == null){
            throw new CompileException("unresolved label '%s'".formatted(labelToken), labelToken);
        }
        return entry.address();
    }

    @Override
    public SectionInfo getSectionInfo(Token labelToken) {
        LabelMapEntry entry = globalLabelMap.get(labelToken.lexeme);
        if(entry == null){
            throw new CompileException("unresolved label '%s'".formatted(labelToken), labelToken);
        }
        return entry.sectionInfo();
    }
}
