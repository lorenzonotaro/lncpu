package com.lnc.assembler.parser;

import com.lnc.assembler.linker.LinkException;

import java.util.List;
import java.util.Set;

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
