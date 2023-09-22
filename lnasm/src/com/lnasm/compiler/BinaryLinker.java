package com.lnasm.compiler;

import com.lnasm.io.ByteArrayChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BinaryLinker extends AbstractLinker{

    private final Set<Block> linkedBlocks;

    private final ByteArrayChannel channel;

    BinaryLinker(Map<String, Short> labels) {
        super(labels);
        linkedBlocks = new HashSet<>();
        this.channel = new ByteArrayChannel(0, false);
    }

    @Override
    byte[] link(Set<Block> blocks) {
        try{
            for (Block block : blocks) {
                checkForOverlap(block);
                linkBlock(block);
                this.linkedBlocks.add(block);
            }
            return this.channel.toByteArray();
        }catch(CompileException e){
            e.log();
            return null;
        }
    }


    private void checkForOverlap(Block block) {
        List<Block> overlappingBlocks = linkedBlocks.stream().filter(b -> blocksOverlap(b, block)).toList();
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
                this.channel.write(ByteBuffer.wrap(encodeable.encode(this, (short) this.channel.position())));
            }
        } catch (IOException e1) {
            throw new Error("I/O while linking" , e1);
        }
    }


}
