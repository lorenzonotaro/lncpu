package com.lnc.assembler.parser.argument;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.common.frontend.Token;

/**
 * Represents an abstract base class for numerical assembly arguments.
 * A numerical argument is any type of argument that has a calculable numerical value
 * during assembly or linking processes. This includes constants, expressions, labels, or other
 * operands that can be resolved to numerical values.
 *
 * Subclasses of {@code NumericalArgument} are expected to implement specific functionality
 * regarding the calculation, encoding, and size determination of the numerical argument.
 *
 * Key characteristics of numerical arguments include:
 * - The ability to resolve a numerical value at a given point in the assembly or linking process.
 * - The potential to size and encode the argument for inclusion in machine instructions or
 *   other output formats.
 * - Compatibility with assembly tools and resolvers, such as label resolvers or
 *   section locators.
 */
public abstract class NumericalArgument extends Argument {

    public NumericalArgument(Token token, Type type) {
        super(token, type);
    }

    public abstract int value(ILabelResolver labelResolver, int instructionAddress);
}
