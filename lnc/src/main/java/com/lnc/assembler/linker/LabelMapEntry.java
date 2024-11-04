package com.lnc.assembler.linker;

import com.lnc.assembler.common.LabelInfo;

public record LabelMapEntry(LabelInfo labelInfo, com.lnc.assembler.common.SectionInfo sectionInfo, int address) {
}
