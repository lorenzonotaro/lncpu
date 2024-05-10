package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.SectionInfo;
import com.lnasm.compiler.parser.CodeElement;
import com.lnasm.compiler.parser.ParsedBlock;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SectionBuilder {

    private int codeLength;

    private final SectionInfo sectionInfo;

    private final ILabelSectionLocator locator;

    private final List<InstructionEntry> instructions;

    private final HashMap<String, LabelMapEntry> labelMap;

    public SectionBuilder(SectionInfo info, ILabelSectionLocator locator){
        this.sectionInfo = info;
        this.locator = locator;
        this.codeLength = 0;
        this.instructions = new LinkedList<>();
        this.labelMap = new HashMap<>();
    }


    public void append(ParsedBlock block){
        for (var instruction : block.instructions) {
            int size = instruction.size(locator);
            instructions.add(new InstructionEntry(this.codeLength, size, instruction));
            this.codeLength += size;

            for (var label : instruction.getLabels()) {
                labelMap.put(label.name(), new LabelMapEntry(label, sectionInfo.start + this.codeLength));
            }
        }
    }

    public Map<String, LabelMapEntry> getLabelMap(){
        return labelMap;
    }

    record InstructionEntry(int index, int size, CodeElement instruction){

    }
}
