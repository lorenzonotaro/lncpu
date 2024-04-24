package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.SectionInfo;

import java.util.Arrays;

public record LinkerConfig(SectionInfo[] sections){
    @Override
    public String toString() {
        return "LinkerConfig{" +
                "sections=" + Arrays.toString(sections) +
                '}';
    }
}
