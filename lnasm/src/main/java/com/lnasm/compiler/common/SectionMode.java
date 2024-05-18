package com.lnasm.compiler.common;

public enum SectionMode {
    // fixed position, set by user in config
    FIXED(0x1fff, 0, "fixed"),

    // automatic positioning, page aligned
    PAGE_ALIGN(0x1fff, 1, "page-aligned"),

    // automatic positioning, fit to page
    PAGE_FIT(0xff, 2, "page-fitted"),

    // automatic positioning, fit wherever possible
    FIT(0x1fff, 3, "fit");


    private final int maxSize;
    private final int precedence;

    SectionMode(int maxSize, int precedence, String name) {
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
