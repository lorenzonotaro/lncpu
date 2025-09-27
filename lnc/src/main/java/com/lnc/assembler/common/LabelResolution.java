package com.lnc.assembler.common;

/**
 * Represents the resolution of a label within a specific section, along with details of the target address
 * and whether it refers to a section name.
 *
 * This immutable record encapsulates the following properties:
 * - The section information where the label resides.
 * - The address associated with the label.
 * - A flag indicating whether the label represents a section name.
 *
 * The {@code LabelResolution} record is primarily used to capture the resolved details
 * of labels during assembly or linking processes.
 *
 * @param sectionInfo     The {@code SectionInfo} object representing the section where the label is resolved.
 * @param address         The integer address associated with the label within the section.
 * @param isSectionName   A boolean indicating if the label corresponds to the section's name.
 */
public record LabelResolution(SectionInfo sectionInfo, int address, boolean isSectionName) {
}
