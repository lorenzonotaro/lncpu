package com.lnasm.compiler;

import com.lnasm.io.ByteArrayChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class Linker {

    private final ByteArrayChannel channel;
    private final Map<String, Short> labels;
    private boolean success;
    private final Set<Block> linkedBlocks;

    Linker(Map<String, Short> labels){
        this.labels = labels;
        this.channel = new ByteArrayChannel(0, false);
        linkedBlocks = new HashSet<>();
    }

    boolean link(Set<Block> blocks){
        this.success = true;
        try{
            for (Block block : blocks) {
                checkForOverlap(block);
                linkBlock(block);
                this.linkedBlocks.add(block);
            }
        }catch(CompileException e){
            e.log();
            this.success = false;
        }
        return this.success;
    }

    private void checkForOverlap(Block block) {
        List<Block> overlappingBlocks = linkedBlocks.stream().filter(b -> blocksOverlap(b, block)).collect(Collectors.toList());
        if(!overlappingBlocks.isEmpty()){
            throw new CompileException(
                            "unable to link: overlapping blocks were found:\n" +
                                    overlappingBlocks.stream().map(b -> overlapDescription(block, b)).collect(Collectors.joining("\n ----- \n")),
                    block.origin);
        }
    }

    private boolean blocksOverlap(Block a, Block b) {
        return (a.startAddress >= b.startAddress && a.startAddress < b.startAddress + b.codeSize) ||
                (a.startAddress + a.codeSize >= b.startAddress && a.startAddress + b.codeSize < b.startAddress);
    }

    private String overlapDescription(Block a, Block b){
        return String.format("\n\t\tblock starting at '%s' (code size = %d bytes) overlaps with\n\t\tblock starting at '%s' (code size = %d bytes)",
                a.origin.formatLocation(),
                a.codeSize,
                b.origin.formatLocation(),
                b.codeSize);
    }

    private void linkBlock(Block block) {
        try {
            this.channel.position(block.startAddress & 0xFFFF);
            for (Encodeable encodeable : block.encodeables) {
                try{
                    this.channel.write(ByteBuffer.wrap(encodeable.encode(this, (short) this.channel.position())));
                }catch(CompileException e){
                    e.log();
                    this.success = false;
                }
            }
        } catch (IOException e1) {
            throw new Error("I/O while linking" , e1);
        }
    }

    public short resolveLabel(String labelName, Token token) {
        if(labels.containsKey(labelName))
            return labels.get(labelName);
        throw new CompileException("unresolved label '" + labelName + "'", token);
    }

    public byte[] getOutput() {
        return channel.toByteArray();
    }
}
