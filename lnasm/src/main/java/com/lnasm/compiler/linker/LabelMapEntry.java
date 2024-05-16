package com.lnasm.compiler.linker;

import com.lnasm.compiler.common.LabelInfo;

public record LabelMapEntry(LabelInfo labelInfo, com.lnasm.compiler.common.SectionInfo sectionInfo, int address) {
}
