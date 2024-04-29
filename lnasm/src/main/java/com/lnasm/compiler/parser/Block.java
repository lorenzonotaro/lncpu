package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.IEncodeable;
import com.lnasm.compiler.common.Token;

import java.util.*;

public class Block {
    public final short startAddress;
    public final List<IEncodeable> encodeables;
    public Token origin;
    public int codeSize;

    public Block(Token origin, short startAddress) {
        this.origin = origin;
        this.startAddress = startAddress;
        this.encodeables = new ArrayList<>();
    }

    boolean addInstruction(IEncodeable instr) {
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
