package com.lnasm.compiler;

import java.util.*;

public class Block {
    final short startAddress;
    final List<Encodeable> encodeables;
    Token origin;
    int codeSize;

    public Block(Token origin, short startAddress) {
        this.origin = origin;
        this.startAddress = startAddress;
        this.encodeables = new ArrayList<>();
    }

    boolean addInstruction(Encodeable instr) {
        if(startAddress + codeSize + instr.size() >= 32768)
            return false;
        encodeables.add(instr);
        codeSize += instr.size();
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return startAddress == block.startAddress && codeSize == block.codeSize && encodeables.equals(block.encodeables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startAddress, encodeables, codeSize);
    }
}
