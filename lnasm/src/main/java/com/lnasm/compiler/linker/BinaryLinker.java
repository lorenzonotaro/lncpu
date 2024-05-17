package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.LabelSectionInfo;
import com.lnasm.compiler.parser.ParseResult;
import com.lnasm.compiler.parser.ParsedBlock;
import com.lnasm.io.ByteArrayChannel;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BinaryLinker extends AbstractLinker<Map<String, ByteArrayChannel>>{
    private Map<String, ByteArrayChannel> outputs;

    public BinaryLinker(LinkerConfig config) {
        super(config);
    }

    @Override
    public boolean link(ParseResult parseResult) {
        try{
            var labelLocator = validateSectionsAndCreateLabelMap(parseResult.blocks());

            var sectionBuilders = makeSectionBuilders(parseResult, labelLocator);

            if(!validateAddressSpace(sectionBuilders))
                return false;

            var labelResolver = makeLabelResolver(sectionBuilders);

            this.outputs = link(sectionBuilders, labelResolver);

            return true;
        }catch(CompileException e) {
            e.log();
        }catch(LinkException e){
            e.log();
        }
        return false;
    }

    @Override
    public Map<String, ByteArrayChannel> getResult() {
        return outputs;
    }

    private Map<String, ByteArrayChannel> link(Map<String, SectionBuilder> sectionBuilders, LabelMapLabelResolver labelResolver) {
        try{
            var result = new HashMap<String, ByteArrayChannel>();
            for (var section : sectionBuilders.values()) {
                var sectionTarget = result.computeIfAbsent(section.getSectionInfo().getType().getDestCode(), k -> new ByteArrayChannel(0, false));
                section.output(sectionTarget, labelResolver);
            }

            return result;
        } catch(IOException e){
            throw new LinkException("error linking sections (%s: %s)".formatted(e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    private boolean validateAddressSpace(Map<String, SectionBuilder> sectionBuilders) {

        boolean success = true;

        var groupedByType = sectionBuilders.values().stream().collect(Collectors.groupingBy(
           s -> s.getSectionInfo().getType().getTarget()
        ));

        for (var entry : groupedByType.entrySet()) {

            var target = entry.getKey();
            var sections = entry.getValue();
            MemoryLayoutManager manager = new MemoryLayoutManager(target);

            var sortedByMode = sections.stream().sorted(Comparator.comparingInt(a -> a.getSectionInfo().getMode().getPrecedence())).toList();

            for(var section : sortedByMode){
                try{
                    section.validateSize();
                    manager.allocate(section);
                } catch(IllegalStateException e){
                    new LinkException("linking failed for section '%s': %s".formatted(section.getSectionInfo().getName(), e.getMessage())).log();
                    success = false;
                } catch(LinkException e){
                    e.log();
                    success = false;
                }

            }

            manager.validate();
        }

        return success;
    }
    private LabelMapLabelResolver makeLabelResolver(Map<String, SectionBuilder> sectionBuilders) {
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
