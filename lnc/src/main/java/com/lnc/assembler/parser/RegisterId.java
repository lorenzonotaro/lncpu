package com.lnc.assembler.parser;

public enum RegisterId {
    RA(true), RB(true), RC(true), RD(true), SS(false), SP(false), DS(false);

    public final boolean generalPurpose;

    private RegisterId(boolean generalPurpose) {
        this.generalPurpose = generalPurpose;
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public static RegisterId fromString(String str) {
        return RegisterId.valueOf(str.toUpperCase());
    }

    public boolean isGeneralPurpose() {
        return generalPurpose;
    }
}
