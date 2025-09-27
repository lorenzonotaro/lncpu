package com.lnc.assembler.common;

/**
 * Represents the different modes that can be used to define the positioning of links.
 * Each mode has a specific configuration that determines its maximum size, precedence,
 * and naming convention.
 */
public enum LinkMode {
    /**
     * Represents a fixed positioning mode for links, as defined by user configuration.
     *
     * This mode is characterized by a predefined position that is explicitly set and does
     * not change dynamically. The positioning is determined using user-provided configurations,
     * making it suitable for scenarios where absolute positioning is required.
     */
    // fixed position, set by user in config
    FIXED(0x2000, 0, "fixed"),

    /**
     * Defines an automatic positioning mode where links are aligned to page boundaries.
     *
     * It is used to position links in a manner that ensures alignment with page boundaries,
     * making it suitable for scenarios where such alignment is a requirement or optimization.
     */
    // automatic positioning, page aligned
    PAGE_ALIGN(0x2000, 1, "page-aligned"),

    /**
     * Represents the "page-fitted" automatic positioning mode, where links are adjusted
     * dynamically to fit within a single page. This mode prioritizes fitting links to
     * pages while adhering to size constraints and assigned precedence.
     *
     * Characteristics:
     * - Maximum size: 0x100
     * - Precedence: 2
     * - Name: "page-fitted"
     */
    // automatic positioning, fit to page
    PAGE_FIT(0x100, 2, "page-fitted"),

    /**
     *
     */
    FIT(0x2000, 3, "fit");


    /**
     * Specifies the maximum size allowed for a given link mode configuration.
     *
     * This parameter is used to constrain or define the limit on the size of links
     * within specific modes. Each link mode has a distinct maximum size, which generally
     * depends on its specific characteristics and usage requirements.
     *
     * The value of maxSize is immutable and is initialized during the construction
     * of a link mode.
     */
    private final int maxSize;
    /**
     * Represents the precedence level associated with a specific link mode.
     *
     * The `precedence` field determines the priority of the corresponding link mode.
     * A lower numeric value means a higher priority in the context it is used.
     * This attribute is immutable and is initialized during the construction of the link mode.
     */
    private final int precedence;

    /**
     * Constructs a new LinkMode instance with the specified maximum size, precedence, and name.
     *
     * @param maxSize the maximum size associated with this link mode
     * @param precedence the precedence level of this link mode, where lower values indicate higher priority
     * @param name the name assigned to this link mode
     */
    LinkMode(int maxSize, int precedence, String name) {
        this.maxSize = maxSize;
        this.precedence = precedence;
    }

    /**
     * Converts the input string to a corresponding LinkMode enum value.
     * The input is transformed to uppercase before attempting to match it
     * to an enum constant.
     *
     * @param input the string representation of the LinkMode value to be converted
     * @return the LinkMode enum constant matching the provided input
     * @throws IllegalArgumentException if the input does not match any enum constant
     * @throws NullPointerException if the input is null
     */
    public static LinkMode from(String input) {
        return LinkMode.valueOf(input.toUpperCase());
    }

    /**
     * Retrieves the maximum size allowed for the current link mode configuration.
     *
     * @return the maximum size associated with this link mode
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Returns the precedence value associated with this link mode.
     *
     * The precedence determines the priority of the mode, where lower numeric values
     * indicate higher priority in processing or selection.
     *
     * @return the precedence level of this link mode
     */
    public int getPrecedence() {
        return precedence;
    }
}
