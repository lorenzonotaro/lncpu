package com.lnasm.compiler.common;

import com.lnasm.compiler.linker.LinkerTarget;

public enum SectionType {
    ROM("rom", LinkerTarget.ROM),

    RAM("ram", LinkerTarget.RAM),

    PAGE0("ram", LinkerTarget.RAM);

    private final String destCode;
    private final LinkerTarget target;

    SectionType(String destCode, LinkerTarget target){
        this.destCode = destCode;
        this.target = target;
    }

    public String getDestCode() {
        return destCode;
    }

    public LinkerTarget getTarget() {
        return target;
    }
}
