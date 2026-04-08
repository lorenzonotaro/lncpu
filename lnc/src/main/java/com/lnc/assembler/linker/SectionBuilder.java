package com.lnc.assembler.linker;

import com.lnc.common.frontend.CompileException;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.LnasmParser;
import com.lnc.assembler.parser.LnasmParsedBlock;
import com.lnc.common.io.ByteArrayChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class SectionBuilder {

    private int codeLength;

    private final SectionInfo sectionInfo;

    private final ILabelSectionLocator locator;

    private final List<InstructionEntry> instructions;

    private HashMap<String, LabelMapEntry> labelMap;

    public int getStart() {
        return sectionStart;
    }

    private int sectionStart = -1;

    private boolean alreadyWritten;

    public SectionBuilder(SectionInfo info, ILabelSectionLocator locator){
        this.sectionInfo = info;
        this.locator = locator;
        this.codeLength = 0;
        this.instructions = new LinkedList<>();
        alreadyWritten = false;
    }


    public void append(LnasmParsedBlock block){


        if(alreadyWritten && !sectionInfo.isMultiWriteAllowed()){
            throw new CompileException("duplicate section '%s' (use 'multi = true' in linker config to append multiple code blocks to the same section)".formatted(sectionInfo.getName()), block.sectionToken);
        }

        for (var instruction : block.instructions) {
            int size = instruction.size(locator);
            instructions.add(new InstructionEntry(this.codeLength, size, instruction));
            this.codeLength += size;
        }

        alreadyWritten = true;
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
    public SectionInfo getSectionInfo() {
        return sectionInfo;
    }

    public void output(ByteArrayChannel sectionTarget, LabelMapLabelResolver labelResolver) throws IOException {
        for (InstructionEntry instruction : instructions) {
            instruction.instruction.getLabels().stream().filter(l -> !l.name().contains(LnasmParser.SUBLABEL_SEPARATOR)).reduce((f, s) -> s).ifPresent(lastParentLabel -> labelResolver.setCurrentParentLabel(lastParentLabel.name()));

            sectionTarget.position(sectionStart + instruction.index - sectionInfo.getTarget().start);
            byte[] buffer = instruction.instruction.encode(labelResolver, sectionStart + instruction.index);
            sectionTarget.write(ByteBuffer.wrap(buffer));
        }
    }

    public int getCodeLength() {
        return codeLength;
    }

    /**
     * Returns top-level label spans as instruction-relative ranges.
     * Each span starts at a top-level label and ends where the next top-level label starts,
     * or at the end of the section for the last one.
     */
    public List<TopLevelLabelSpan> getTopLevelLabelSpans() {
        record Boundary(String labelName, int offset) {}

        List<Boundary> boundaries = new ArrayList<>();
        for (InstructionEntry entry : instructions) {
            for (var label : entry.instruction.getLabels()) {
                if (!label.name().contains(LnasmParser.SUBLABEL_SEPARATOR)) {
                    boundaries.add(new Boundary(label.name(), entry.index));
                }
            }
        }

        if (boundaries.isEmpty()) {
            return List.of();
        }

        List<TopLevelLabelSpan> spans = new ArrayList<>(boundaries.size());
        for (int i = 0; i < boundaries.size(); i++) {
            Boundary current = boundaries.get(i);
            int endOffsetExclusive = i + 1 < boundaries.size()
                    ? boundaries.get(i + 1).offset
                    : codeLength;
            spans.add(new TopLevelLabelSpan(current.labelName, current.offset, endOffsetExclusive));
        }
        return spans;
    }

    public boolean isAlreadyWritten() {
        return alreadyWritten;
    }

    public Descriptor getDescriptor() {
        return new Descriptor(sectionInfo, sectionStart, codeLength);
    }

    record InstructionEntry(int index, int size, CodeElement instruction){

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SectionBuilder that = (SectionBuilder) o;

        if (codeLength != that.codeLength) return false;
        if (sectionStart != that.sectionStart) return false;
        return sectionInfo.equals(that.sectionInfo);
    }

    @Override
    public int hashCode() {
        int result = codeLength;
        result = 31 * result + sectionInfo.hashCode();
        result = 31 * result + sectionStart;
        return result;
    }

    public record Descriptor(SectionInfo sectionInfo, int start, int length) {
    }

    public record TopLevelLabelSpan(String labelName, int startOffset, int endOffsetExclusive) {
    }
}
