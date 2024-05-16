package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.LabelSectionInfo;
import com.lnasm.compiler.common.SectionType;
import com.lnasm.compiler.parser.ParseResult;
import com.lnasm.compiler.parser.ParsedBlock;
import com.lnasm.io.ByteArrayChannel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BinaryLinker extends AbstractLinker{
    public BinaryLinker(LinkerConfig config) {
        super(config);
    }

    @Override
    public byte[] link(ParseResult parseResult) {
        var labelLocator = validateSectionsAndCreateLabelMap(parseResult.blocks());

        var sectionBuilders = makeSectionBuilders(parseResult, labelLocator);

        validateAddressSpace(sectionBuilders);

        var labelResolver = makeLabelResolver(sectionBuilders);

        try {
            var outputs = link(sectionBuilders, labelResolver);
            return outputs.get(SectionType.ROM.getDestCode()).toByteArray();
        } catch (IOException e) {
            throw new LinkException("error linking sections (%s: %s)".formatted(e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    private Map<String, ByteArrayChannel> link(Map<String, SectionBuilder> sectionBuilders, ILabelResolver labelResolver) throws IOException {
        var result = new HashMap<String, ByteArrayChannel>();
        for (var section : sectionBuilders.values()) {
            var sectionTarget = result.computeIfAbsent(section.getSectionInfo().getType().getDestCode(), k -> new ByteArrayChannel(0, false));
            section.output(sectionTarget, labelResolver);
        }

        return result;
    }

    private void validateAddressSpace(Map<String, SectionBuilder> sectionBuilders) {

        // calculate each section start address and check if it respects its max size
        for (var entry : sectionBuilders.values()) {
            // for now, simply set the start address to the section start address in the config
            // TODO: implement section modes in linker config
            entry.setSectionStart(entry.getSectionInfo().getStart());

            entry.validateSize();
        }

        //check if any two sections overlap
        for (var entry : sectionBuilders.entrySet()) {
            for (var other : sectionBuilders.entrySet()) {
                if(entry == other){
                    continue;
                }
                if(entry.getValue().overlaps(other.getValue())){
                    throw new LinkException("sections '%s' and '%s' overlap".formatted(entry.getKey(), other.getKey()));
                }
            }
        }
    }

    private ILabelResolver makeLabelResolver(Map<String, SectionBuilder> sectionBuilders) {
        // build global label map
        Map<String, LabelMapEntry> globalLabelMap = new HashMap<>();

        for (var sectionBuilder : sectionBuilders.values()) {
            sectionBuilder.buildLabelMap();
            for (Map.Entry<String, LabelMapEntry> labelEntry : sectionBuilder.getLabelMap().entrySet()) {
                LabelMapEntry previous;
                if((previous = globalLabelMap.putIfAbsent(labelEntry.getKey(), labelEntry.getValue())) != null) {
                    throw new CompileException(
                            "duplicate label '%s' (first defined at %s)".formatted(previous.labelInfo().name(), previous.labelInfo().token().formatLocation()),
                            labelEntry.getValue().labelInfo().token());
                }
            }
        }

        return new LabelMapLabelResolver(globalLabelMap);
    }

    private Map<String, SectionBuilder> makeSectionBuilders(ParseResult parseResult, LinkerLabelSectionLocator labelLocator) {
        var sectionBuilders = new HashMap<String, SectionBuilder>();

        for (ParsedBlock block : parseResult.blocks()) {
            var sectionInfo = getConfig().getSectionInfo(block.sectionName);
            if (sectionInfo == null){
                throw new CompileException("section not found in linker config", block.sectionToken);
            }else{
                var sectionBuilder = sectionBuilders.computeIfAbsent(block.sectionName, k -> new SectionBuilder(sectionInfo, labelLocator));
                sectionBuilder.append(block);
            }
        }

        return sectionBuilders;
    }

    private LinkerLabelSectionLocator validateSectionsAndCreateLabelMap(ParsedBlock[] blocks) {
        LinkerLabelSectionLocator locator = new LinkerLabelSectionLocator();
        for (ParsedBlock block : blocks) {
            var sectionInfo = getConfig().getSectionInfo(block.sectionName);
            if (sectionInfo == null){
                throw new CompileException("section not found in linker config", block.sectionToken);
            }else{
                for (var instruction : block.instructions) {
                    for (var label : instruction.getLabels()) {
                        locator.put(label.name(), new LabelSectionInfo(label, sectionInfo));
                    }
                }
            }
        }
        return locator;
    }

}
