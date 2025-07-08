package com.lnc.assembler.parser;

public enum RegisterId {
    RA(true),
    RB(true),
    RC(true),
    RD(true),
    SS(false),
    SP(false),
    BP(false),
    DS(false);

    public final boolean generalPurpose;

    RegisterId(boolean generalPurpose) {
        this.generalPurpose = generalPurpose;
    }

    public static RegisterId fromString(String str) {
        return RegisterId.valueOf(str.toUpperCase());
    }

    public boolean isGeneralPurpose() {
        return generalPurpose;
    }
}
