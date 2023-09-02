package com.lnasm.compiler;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ImmediateLinker extends AbstractLinker {
    private final Map<Short, String> reverseLookupLabels;
    private final int longestLabelLength;

    public ImmediateLinker(Map<String, Short> labels) {
        super(labels);
        this.reverseLookupLabels = labels.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        this.longestLabelLength = labels.keySet().stream().mapToInt(String::length).max().orElse(0);
    }

    @Override
    byte[] link(Set<Block> blocks) {
        try{
            StringBuilder sb = new StringBuilder();

            List<Block> sortedBlocks = blocks.stream().sorted(Comparator.comparingInt(b -> b.startAddress)).toList();

            for(Block block : sortedBlocks){
                short addr = block.startAddress;
                sb.append(String.format("###### Origin: 0x%06x (block at %s:%d) ###### \n\n", block.startAddress & 0xFFFFFF, block.origin.file, block.origin.line));
                for(Encodeable encodeable : block.encodeables){
                    byte[] encoded = encodeable.encode(this, addr);
                    StringBuilder params = new StringBuilder();
                    for (int i = 1; i < encoded.length; i++) {
                        params.append(String.format("%02x ", encoded[i] & 0xFF));
                    }
                    String instructionDesc = String.format("%02x (%s)", encoded[0], OpcodeMap.getImmediateName(encoded[0]));
                    sb.append(String.format("%" + longestLabelLength + "s %06x:\t%-20s %16s", reverseLookupLabels.getOrDefault(addr, ""), addr & 0xFFFFFF, instructionDesc, params));
                    sb.append("\n");
                    addr += encodeable.size();
                }
                sb.append("\n\n");
            }

            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }catch(CompileException e){
            e.log();
            return null;
        }


    }
}
