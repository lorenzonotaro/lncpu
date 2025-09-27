package com.lnc.assembler.common;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;

/**
 * Represents an entity that can be encoded into a binary format or byte array
 * and whose size in memory can be calculated.
 *
 * This interface is primarily intended for use in systems that require binary
 * encoding of instructions, arguments, or other components, such as in
 * assembly language interpreters, compilers, or related tools.
 */
public interface IEncodeable {
    /**
     * Calculates the size of the encoded representation of an object in memory.
     *
     * @param sectionLocator an instance of ILabelSectionLocator that provides label-to-section resolution
     *                       information, which may be required for calculating the size.
     * @return the size of the encoded object in bytes as an integer.
     */
    int size(ILabelSectionLocator sectionLocator);

    /**
     * Encodes an entity to a binary format or byte array based on the provided label resolver,
     * link information, and the address of the instruction being processed.
     *
     * @param labelResolver the resolver used to resolve symbolic labels to their addresses or values
     * @param instructionAddress the memory address of the current instruction to be encoded
     * @return a byte array representing the binary encoding of the entity
     */
    byte[] encode(ILabelResolver labelResolver, int instructionAddress);
}
