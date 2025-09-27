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

/**
 * Resolves labels based on a global label map and section builders, implementing the {@link ILabelResolver} interface.
 * This class is responsible for determining the address, section information, and other metadata about labels
 * within an assembly or linking context.
 *
 * The resolution can involve both global labels and scoped sublabels (determined by their parent label).
 * Additionally, it handles sections for linkage and symbol table management.
 *
 * Key responsibilities include:
 * - Resolving tokens to label or section information.
 * - Managing parent label context for proper sublabel resolution.
 * - Providing mechanisms to generate a reverse symbol table and export label mappings.
 */
public class LabelMapLabelResolver implements ILabelResolver {
    private final Map<String, LabelMapEntry> globalLabelMap;
    private final Map<String, SectionBuilder> sectionBuilders;

    private String currentParentLabel;

    public LabelMapLabelResolver(Map<String, LabelMapEntry> globalLabelMap, Map<String, SectionBuilder> sectionBuilders) {
        this.globalLabelMap = globalLabelMap;
        this.sectionBuilders = sectionBuilders;
    }

    /**
     * Resolves the given label token by determining its corresponding section and address.
     * This method performs a lookup to check if the label exists in the global label map
     * or is associated with a section. If the label cannot be resolved, a {@code CompileException}
     * is thrown. Additionally, it handles special cases where the label corresponds to data page sections.
     *
     * @param labelToken The {@code Token} representing the label to be resolved. The token contains
     *                   the lexeme of the label to locate, alongside additional metadata such as
     *                   its type and location in the source code.
     * @return A {@code LabelResolution} object encapsulating the resolved section, address,
     *         and whether the label refers to a section name. If the label corresponds to a section
     *         start name, the resolution will indicate so.
     * @throws CompileException If the label cannot be resolved (e.g., if it's undefined) or if
     *                          the label references a virtual data page section start.
     */
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

            var entry = globalLabelMap.get(currentParentLabel + LnasmParser.SUBLABEL_SEPARATOR + lexeme);
            if(entry != null){
                return entry;
            }
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

    public Map<String, LabelMapEntry> getEntriesFor(Set<String> exportedLabels) {
        var map = new HashMap<String, LabelMapEntry>();
        for(var label : exportedLabels){
            var entry = globalLabelMap.get(label);
            if(entry == null){
                throw new RuntimeException("cannot export unresolved label '%s'".formatted(label));
            }
            map.put(label, entry);
        }
        return map;
    }
}
