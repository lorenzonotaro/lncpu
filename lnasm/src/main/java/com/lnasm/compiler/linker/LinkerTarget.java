package com.lnasm.compiler.linker;

public enum LinkerTarget {
    ROM(0x0, 0x1fff), RAM(0x2000, 0x3fff);

    final int start;

    final int end;

    LinkerTarget(int start, int end){
        this.start = start;
        this.end = end;
    }
}
