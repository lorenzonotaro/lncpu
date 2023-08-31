package com.lnasm.compiler;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImmediateLinker extends AbstractLinker {
    public ImmediateLinker(Map<String, Short> labels) {
        super(labels);
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
                    sb.append(String.format("%06x:\t%-20s %16s", addr & 0xFFFFFF, instructionDesc, params));
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
