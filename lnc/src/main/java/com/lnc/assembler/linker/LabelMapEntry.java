package com.lnc.assembler.linker;

import com.lnc.assembler.common.LabelInfo;
import com.lnc.assembler.common.SectionInfo;

import java.io.Serializable;

public record LabelMapEntry(LabelInfo labelInfo, SectionInfo sectionInfo, int address) implements Serializable {
}
