package com.lnc.assembler.parser;

import java.util.ArrayList;
import java.util.List;

public record LnasmParseResult(List<LnasmParsedBlock> blocks) {
    public LnasmParseResult join(List<LnasmParsedBlock> results) {

        blocks.addAll(results);

        return this;
    }
}
