package com.lnc.assembler.parser;

import com.lnc.assembler.linker.LinkException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the result of parsing a collection of LNASM source code blocks.
 * This record encapsulates both the parsed blocks and the exported labels.
 * It also provides functionality to combine parsing results while ensuring
 * no duplicate exported labels are present.
 *
 * The class primarily handles:
 * - Managing a collection of parsed blocks represented by {@code LnasmParsedBlock}.
 * - Tracking exported labels as a set of unique strings.
 * - Supporting the joining of additional parse results with validation of label uniqueness.
 *
 * @param blocks          A list of parsed blocks from the assembly source.
 * @param exportedLabels  A map of labels that are exported from the parsed blocks, where the key is the exported name and the name is the actual name in the unit.
 */
public record LnasmParseResult(List<LnasmParsedBlock> blocks, Map<String, String> exportedLabels) {
    public LnasmParseResult join(List<LnasmParsedBlock> results, Map<String, String> exportedLabels) {

        blocks.addAll(results);

        for (String s : exportedLabels.keySet()) {
            if(this.exportedLabels.containsKey(s)){
                throw new LinkException("Label '" + s + "' is exported multiple times");
            }
            this.exportedLabels.put(s, exportedLabels.get(s));
        }

        return this;
    }
}
