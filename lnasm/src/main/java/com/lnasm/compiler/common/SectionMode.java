package com.lnasm.compiler.common;

public enum SectionMode {
    // fixed position, set by user in config
    FIXED(0x1fff, 0),

    // automatic positioning, page aligned
    PAGE_ALIGN(0xff, 1),

    // automatic positioning, fit to page
    PAGE_FIT(0xff, 2),

    // automatic positioning, fit wherever possible
    FIT(0x1fff, 3);


    private final int maxSize;
    private final int precedence;

    SectionMode(int maxSize, int precedence) {
        this.maxSize = maxSize;
        this.precedence = precedence;
    }

    public static SectionMode from(String input) {
        return SectionMode.valueOf(input.toUpperCase());
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getPrecedence() {
        return precedence;
    }
}
