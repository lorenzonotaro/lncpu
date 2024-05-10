package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.LabelSectionInfo;
import com.lnasm.compiler.parser.ParseResult;
import com.lnasm.compiler.parser.ParsedBlock;

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

        var labelResolver = makeLabelResolver(labelLocator, sectionBuilders);

        return new byte[0];
    }

    private ILabelResolver makeLabelResolver(LinkerLabelSectionLocator labelLocator, Map<String, SectionBuilder> sectionBuilders) {
        // build global label map
        Map<String, LabelMapEntry> globalLabelMap = new HashMap<>();

        for (var entry : sectionBuilders.entrySet()) {
            for (Map.Entry<String, LabelMapEntry> labelEntry : entry.getValue().getLabelMap().entrySet()) {
                LabelMapEntry previous;
                if((previous = globalLabelMap.putIfAbsent(labelEntry.getKey(), labelEntry.getValue())) != null) {
                    throw new CompileException(
                            "duplicate label '%s' (first defined at %s)".formatted(previous.labelInfo().name(), previous.labelInfo().token().formatLocation()),
                            labelEntry.getValue().labelInfo().token());
                }
            }
        }

        return new LabelMapLabelResolver(labelLocator, globalLabelMap);
    }

    private Map<String, SectionBuilder> makeSectionBuilders(ParseResult parseResult, LinkerLabelSectionLocator labelLocator) {
        var sectionBuilders = new HashMap<String, SectionBuilder>();

        for (ParsedBlock block : parseResult.blocks()) {
            var sectionInfo = getConfig().getSectionInfo(block.sectionName);
            if (sectionInfo == null){
                throw new CompileException("section not found in linker config", block.sectionToken);
            }else{
                var sectionBuilder = sectionBuilders.get(block.sectionName);
                if (sectionBuilder == null){
                    sectionBuilder = new SectionBuilder(sectionInfo, labelLocator);
                    sectionBuilders.put(block.sectionName, sectionBuilder);
                }
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
