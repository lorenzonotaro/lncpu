package com.lnc.cc.codegen;

import com.lnc.assembler.common.SectionInfo;

public record CompilerOutput(String code, SectionInfo sectionInfo) {
}
