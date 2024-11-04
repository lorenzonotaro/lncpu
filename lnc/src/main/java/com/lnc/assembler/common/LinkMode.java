package com.lnc.assembler.common;

public enum LinkMode {
    // fixed position, set by user in config
    FIXED(0x2000, 0, "fixed"),

    // automatic positioning, page aligned
    PAGE_ALIGN(0x2000, 1, "page-aligned"),

    // automatic positioning, fit to page
    PAGE_FIT(0x100, 2, "page-fitted"),

    // automatic positioning, fit wherever possible
    FIT(0x2000, 3, "fit");


    private final int maxSize;
    private final int precedence;

    LinkMode(int maxSize, int precedence, String name) {
        this.maxSize = maxSize;
        this.precedence = precedence;
    }

    public static LinkMode from(String input) {
        return LinkMode.valueOf(input.toUpperCase());
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getPrecedence() {
        return precedence;
    }
}
