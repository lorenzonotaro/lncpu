package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.SectionInfo;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.parser.LnasmParser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LabelMapLabelResolver implements ILabelResolver {
    private final Map<String, LabelMapEntry> globalLabelMap;
    private final Map<String, SectionBuilder> sectionBuilders;

    private String currentParentLabel;

    public LabelMapLabelResolver(Map<String, LabelMapEntry> globalLabelMap, Map<String, SectionBuilder> sectionBuilders) {
        this.globalLabelMap = globalLabelMap;
        this.sectionBuilders = sectionBuilders;
    }

    @Override
    public int resolve(Token labelToken) {
        LabelMapEntry entry = computeLabel(labelToken.lexeme);
        if(entry == null){
            var section = sectionBuilders.get(labelToken.lexeme);

            if(section != null){
                if(section.getSectionInfo().isDataPage() && section.getSectionInfo().isVirtual()){
                    throw new CompileException("cannot evaluate section start for virtual data page section '%s'".formatted(labelToken.lexeme), labelToken);
                }
                return section.getStart();
            }

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

            var section = sectionBuilders.get(labelToken.lexeme);

            if(section != null){
                return section.getSectionInfo();
            }

            throw new CompileException("unresolved label '%s'".formatted(labelToken.lexeme), labelToken);
        }
        return entry.sectionInfo();
    }

    public void setCurrentParentLabel(String name) {
        this.currentParentLabel = name;
    }

    public Map<Integer, Set<String>> createReverseSymbolTable() {
        var reverseSymbolTable = new HashMap<Integer, Set<String>>();

        for (var entry : globalLabelMap.entrySet()) {
            reverseSymbolTable.computeIfAbsent(entry.getValue().address(), k -> new HashSet<>()).add(entry.getKey());
        }

        return reverseSymbolTable;
    }
}
