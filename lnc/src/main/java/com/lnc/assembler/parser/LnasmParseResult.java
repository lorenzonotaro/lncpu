package com.lnc.assembler.parser;

import com.lnc.assembler.linker.LinkException;

import java.util.List;
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
 * @param exportedLabels  A set of labels that are exported from the parsed blocks.
 */
public record LnasmParseResult(List<LnasmParsedBlock> blocks, Set<String> exportedLabels) {
    public LnasmParseResult join(List<LnasmParsedBlock> results, List<String> list) {

        blocks.addAll(results);

        for (String s : list) {
            if(s == null || s.isEmpty())
                continue;
            if(this.exportedLabels.contains(s)){
                throw new LinkException("Label '" + s + "' is exported multiple times");
            }
            exportedLabels.add(s);
        }

        return this;
    }
}
