package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.SectionInfo;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.parser.LnasmParser;

import java.util.Map;

public class LabelMapLabelResolver implements ILabelResolver {
    private final Map<String, LabelMapEntry> globalLabelMap;

    private String currentParentLabel;

    public LabelMapLabelResolver(Map<String, LabelMapEntry> globalLabelMap) {
        this.globalLabelMap = globalLabelMap;
    }

    @Override
    public int resolve(Token labelToken) {
        LabelMapEntry entry = computeLabel(labelToken.lexeme);
        if(entry == null){
            throw new CompileException("unresolved label '%s'".formatted(labelToken.lexeme), labelToken);
        }
        return entry.address();
    }

    private LabelMapEntry computeLabel(String lexeme) {
        if(currentParentLabel != null && lexeme.startsWith(LnasmParser.SUBLABEL_INITIATOR)){
            lexeme = currentParentLabel + LnasmParser.SUBLABEL_SEPARATOR + lexeme;
        }
        return globalLabelMap.get(lexeme);
    }

    @Override
    public SectionInfo getSectionInfo(Token labelToken) {
        LabelMapEntry entry = computeLabel(labelToken.lexeme);
        if(entry == null){
            throw new CompileException("unresolved label '%s'".formatted(labelToken.lexeme), labelToken);
        }
        return entry.sectionInfo();
    }

    public void setCurrentParentLabel(String name) {
        this.currentParentLabel = name;
    }
}
