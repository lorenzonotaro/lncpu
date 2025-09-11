package com.lnc.assembler.linker;

import com.lnc.common.frontend.CompileException;
import com.lnc.assembler.common.LabelSectionInfo;
import com.lnc.assembler.parser.LnasmParseResult;
import com.lnc.assembler.parser.LnasmParsedBlock;
import com.lnc.common.io.ByteArrayChannel;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BinaryLinker extends AbstractLinker<Map<LinkTarget, ByteArrayChannel>>{
    private Map<LinkTarget, ByteArrayChannel> outputs;

    private LabelMapLabelResolver labelResolver;

    private final LinkInfo linkInfo;

    private Map<String, SectionBuilder> sectionBuilders;
    private final List<String> externalSymTablesFiles;

    public BinaryLinker(LinkerConfig config) {
        this(config, Collections.emptyList());
    }

    public BinaryLinker(LinkerConfig config, List<String> externalSymTablesFiles){
        super(config);
        this.externalSymTablesFiles = externalSymTablesFiles;
        linkInfo = new LinkInfo();
    }

    @Override
    public boolean link(LnasmParseResult parseResult) {
        try{

            Map<String, LabelMapEntry> externalSymbols = loadExternalSymbols();

            var labelLocator = validateSectionsAndCreateLabelMap(parseResult.blocks(), externalSymbols);

            this.sectionBuilders = makeSectionBuilders(parseResult, labelLocator);

            if(!validateAddressSpace(sectionBuilders))
                return false;

            this.labelResolver = makeLabelResolver(sectionBuilders, externalSymbols);

            this.outputs = link(sectionBuilders, labelResolver);

            return true;
        }catch(CompileException e) {
            e.log();
        }catch(LinkException e){
            e.log();
        }
        return false;
    }

    private Map<String, LabelMapEntry> loadExternalSymbols() {
        Map<String, LabelMapEntry> externalSymbols = null;
        if (externalSymTablesFiles != null && !externalSymTablesFiles.isEmpty()){
            externalSymbols = new HashMap<>();
            for(String externalSymTableFile : externalSymTablesFiles){
                var extSymTable = ExternalSymbolTableIO.read(externalSymTableFile);
                for(var entry : extSymTable.entrySet()){
                    if(externalSymbols.putIfAbsent(entry.getKey(), entry.getValue()) != null){
                        throw new LinkException("duplicate external label '%s' in external symbol table '%s'".formatted(entry.getKey(), externalSymTableFile));
                    }
                }
            }
        }
        return externalSymbols;
    }

    @Override
    public Map<LinkTarget, ByteArrayChannel> getResult() {
        return outputs;
    }

    private Map<LinkTarget, ByteArrayChannel> link(Map<String, SectionBuilder> sectionBuilders, LabelMapLabelResolver labelResolver) {
        try{
            var result = new HashMap<LinkTarget, ByteArrayChannel>();
            for (var section : sectionBuilders.values()) {
                var sectionTarget = result.computeIfAbsent(section.getSectionInfo().getTarget(), k -> new ByteArrayChannel(0, false));
                section.output(sectionTarget, labelResolver, linkInfo);
            }

            return result;
        } catch(IOException e){
            throw new LinkException("error linking sections (%s: %s)".formatted(e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    private boolean validateAddressSpace(Map<String, SectionBuilder> sectionBuilders) {

        boolean success = true;

        var groupedByType = sectionBuilders.values().stream().collect(Collectors.groupingBy(
           s -> s.getSectionInfo().getTarget()
        ));

        for (var entry : groupedByType.entrySet()) {
            var target = entry.getKey();
            var sections = entry.getValue();

            if(target.equals(LinkTarget.__VIRTUAL__)){
                // do not consider virtual sections in the memory manager
                for(var section : sections){
                    section.setSectionStart(0);
                }
            }

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
    private LabelMapLabelResolver makeLabelResolver(Map<String, SectionBuilder> sectionBuilders, Map<String, LabelMapEntry> externalSymbols) {
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
        // load external symbol tables
        if(externalSymbols != null){
            for (var entry : externalSymbols.entrySet()) {
                LabelMapEntry previous;
                if((previous = globalLabelMap.putIfAbsent(entry.getKey(), entry.getValue())) != null) {
                    throw new CompileException(
                            "duplicate external label '%s' (first defined at %s)".formatted(previous.labelInfo().name(), previous.labelInfo().token().formatLocation()),
                            entry.getValue().labelInfo().token());
                }
            }
        }

        return new LabelMapLabelResolver(globalLabelMap, sectionBuilders);
    }

    private Map<String, SectionBuilder> makeSectionBuilders(LnasmParseResult parseResult, LinkerLabelSectionLocator labelLocator) {
        var sectionBuilders = new HashMap<String, SectionBuilder>();

        for (var sectionInfo : getConfig().getSectionInfos()) {
            sectionBuilders.put(sectionInfo.getName(), new SectionBuilder(sectionInfo, labelLocator));
        }

        for (LnasmParsedBlock block : parseResult.blocks()){
            SectionBuilder sb = sectionBuilders.get(block.sectionName);

            if(sb == null){
                throw new CompileException("section not found in linker config", block.sectionToken);
            }

            sb.append(block);
        }

        return sectionBuilders;
    }

    private LinkerLabelSectionLocator validateSectionsAndCreateLabelMap(List<LnasmParsedBlock> blocks, Map<String, LabelMapEntry> externalSymbols) {
        LinkerLabelSectionLocator locator = new LinkerLabelSectionLocator(getConfig());
        for (LnasmParsedBlock block : blocks) {
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

        if(externalSymbols != null){
            for (var entry : externalSymbols.entrySet()) {
                if(locator.putIfAbsent(entry.getKey(), new LabelSectionInfo(entry.getValue().labelInfo(), entry.getValue().sectionInfo())) != null){
                    throw new CompileException("duplicate external label '%s'".formatted(entry.getKey()), entry.getValue().labelInfo().token());
                }
            }
        }


        return locator;
    }

    public Map<Integer, Set<String>> createReverseSymbolTable() {
        if(labelResolver == null)
            throw new IllegalStateException("linker not setup");
        return labelResolver.createReverseSymbolTable();
    }

    public List<SectionBuilder.Descriptor> createSectionDescriptors(LinkTarget target) {
        if(labelResolver == null)
            throw new IllegalStateException("linker not setup");
        return this.sectionBuilders.values().stream()
                .filter(s -> s.getSectionInfo().getTarget() == target)
                .map(SectionBuilder::getDescriptor)
                .sorted(Comparator.comparingInt(SectionBuilder.Descriptor::start)).toList();
    }

    public LabelMapLabelResolver getLabelResolver() {
        return labelResolver;
    }
}
