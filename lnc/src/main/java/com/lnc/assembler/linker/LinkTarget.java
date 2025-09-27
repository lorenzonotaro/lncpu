package com.lnc.assembler.linker;

/**
 * An enumeration representing different memory regions, each defined by its starting
 * and ending address and its role in the memory architecture.
 *
 * This allows logical segmentation of memory blocks and provides utility methods
 * to evaluate memory addresses against these regions, retrieve related information,
 * and manipulate memory distribution in defined ranges.
 */
public enum LinkTarget {
    ROM(0x0, 0x1fff, true),
    RAM(0x2000, 0x3fff, false),
    D0(0x4000, 0x5fff, false),
    D1(0x6000, 0x7fff, false),
    D2(0x8000, 0x9fff, false),
    D3(0xa000, 0xbfff, false),
    D4(0xc000, 0xdfff, false),
    D5(0xe000, 0xffff, false),
    __VIRTUAL__(0, 0xFFFF, false);

    public final int start;

    public final int end;

    LinkTarget(int start, int end, boolean defaultOutput){
        this.start = start;
        this.end = end;
    }

    public boolean contains(int address){
        return address >= start && address <= end;
    }

    public static LinkTarget fromAddress(int address) {
        for(var target : LinkTarget.values()){
            if(target.contains(address))
                return target;
        }
        throw new IllegalArgumentException("address %04x out of the address space".formatted(address));
    }

    public int getMaxSize() {
        return end - start + 1;
    }
}
