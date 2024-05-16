package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.LabelInfo;
import com.lnasm.compiler.common.SectionInfo;
import com.lnasm.compiler.parser.CodeElement;
import com.lnasm.compiler.parser.LnasmParser;
import com.lnasm.compiler.parser.ParsedBlock;
import com.lnasm.io.ByteArrayChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SectionBuilder {

    private int codeLength;

    private final SectionInfo sectionInfo;

    private final ILabelSectionLocator locator;

    private final List<InstructionEntry> instructions;

    private HashMap<String, LabelMapEntry> labelMap;
    private int sectionStart = -1;

    public SectionBuilder(SectionInfo info, ILabelSectionLocator locator){
        this.sectionInfo = info;
        this.locator = locator;
        this.codeLength = 0;
        this.instructions = new LinkedList<>();
    }


    public void append(ParsedBlock block){
        for (var instruction : block.instructions) {
            int size = instruction.size(locator);
            instructions.add(new InstructionEntry(this.codeLength, size, instruction));
            this.codeLength += size;
        }
    }

    public void setSectionStart(int sectionStart){
        this.sectionStart = sectionStart;
    }

    public void buildLabelMap(){

        if(sectionStart == -1){
            throw new IllegalStateException("section start not set");
        }

        labelMap = new HashMap<>();
        for (var entry : instructions) {
            for (var label : entry.instruction.getLabels()) {
                labelMap.put(label.name(), new LabelMapEntry(label, sectionInfo, sectionStart + entry.index));
            }
        }
    }

    public void validateSize(){
        if(codeLength > sectionInfo.getMaxSize()){
            throw new LinkException("section '%s' exceeds max size (%d > %d)".formatted(sectionInfo.getName(), codeLength, sectionInfo.getMaxSize()));
        }
    }

    public Map<String, LabelMapEntry> getLabelMap(){
        if(labelMap == null){
            throw new IllegalStateException("label map not built");
        }
        return labelMap;
    }

    public boolean overlaps(SectionBuilder other) {

        if(this.sectionStart == -1 || other.sectionStart == -1){
            throw new IllegalStateException("section start not set");
        }

        return this.sectionStart < other.sectionStart + other.codeLength && other.sectionStart < this.sectionStart + this.codeLength;

    }

    public SectionInfo getSectionInfo() {
        return sectionInfo;
    }

    public void output(ByteArrayChannel sectionTarget, LabelMapLabelResolver labelResolver) throws IOException {
        for (InstructionEntry instruction : instructions) {
            instruction.instruction.getLabels().stream().filter(l -> !l.name().contains(LnasmParser.SUBLABEL_SEPARATOR)).reduce((f, s) -> s).ifPresent(lastParentLabel -> labelResolver.setCurrentParentLabel(lastParentLabel.name()));

            sectionTarget.position(sectionStart + instruction.index - sectionInfo.getType().getStart());
            byte[] buffer = instruction.instruction.encode(labelResolver, sectionStart + instruction.index);
            sectionTarget.write(ByteBuffer.wrap(buffer));
        }
    }

    record InstructionEntry(int index, int size, CodeElement instruction){

    }
}
