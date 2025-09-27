package com.lnc.assembler.parser;

/**
 * The RegisterId enum defines a set of identifiers representing CPU registers.
 * Each register is classified as either general-purpose or not general-purpose.
 *
 * General-purpose registers are intended for general use in computations,
 * while the others may be reserved for specific purposes such as management
 * of stack or segment operations.
 */
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
