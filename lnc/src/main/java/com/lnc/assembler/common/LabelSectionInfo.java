package com.lnc.assembler.common;

/**
 * Encapsulates information that combines a label and its associated section.
 * This class represents the pairing of a `LabelInfo` object, containing details about a specific label,
 * with a `SectionInfo` object, containing metadata associated with a particular section.
 *
 * This can be used to associate labels with their corresponding sections in the context of assembly or linking processes.
 *
 * @param labeLInfo Contains the label-related metadata including name and token information.
 * @param sectionInfo Contains the section-related metadata including section name, starting address, and other section details.
 */
public record LabelSectionInfo(LabelInfo labeLInfo, SectionInfo sectionInfo){
}
