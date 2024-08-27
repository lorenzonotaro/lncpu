package com.lnc.assembler.linker;

import com.lnc.common.frontend.CompileException;
import com.lnc.assembler.common.LabelResolution;
import com.lnc.assembler.common.SectionResolution;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.parser.LnasmParser;

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
    public LabelResolution resolve(Token labelToken) {
        LabelMapEntry entry = computeLabel(labelToken.lexeme);
        if(entry == null){
            var section = sectionBuilders.get(labelToken.lexeme);

            if(section != null){
                if(section.getSectionInfo().isDataPage() && section.getSectionInfo().isVirtual()){
                    throw new CompileException("cannot evaluate section start for virtual data page section '%s'".formatted(labelToken.lexeme), labelToken);
                }
                return new LabelResolution(section.getSectionInfo(), section.getStart(), true);
            }

            throw new CompileException("unresolved label '%s'".formatted(labelToken.lexeme), labelToken);
        }
        return new LabelResolution(entry.sectionInfo(), entry.address(), false);
    }

    private LabelMapEntry computeLabel(String lexeme) {
        if(currentParentLabel != null && lexeme.startsWith(LnasmParser.SUBLABEL_INITIATOR)){
            lexeme = currentParentLabel + LnasmParser.SUBLABEL_SEPARATOR + lexeme;
        }
        return globalLabelMap.get(lexeme);
    }

    @Override
    public SectionResolution getSectionInfo(Token labelToken) {
        LabelMapEntry entry = computeLabel(labelToken.lexeme);
        if(entry == null){

            var section = sectionBuilders.get(labelToken.lexeme);

            if(section != null){
                return new SectionResolution(section.getSectionInfo(), true);
            }

            throw new CompileException("unresolved label '%s'".formatted(labelToken.lexeme), labelToken);
        }
        return new SectionResolution(entry.sectionInfo(), false);
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
