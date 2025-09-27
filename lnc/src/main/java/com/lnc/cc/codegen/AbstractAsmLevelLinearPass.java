package com.lnc.cc.codegen;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.common.ExtendedListIterator;

import java.util.LinkedList;

/**
 * AbstractAsmLevelLinearPass is an abstract base class for implementing linear passes
 * over a linked list of assembly-level {@code CodeElement} objects.
 *
 * A linear pass traverses the list of {@code CodeElement} instances sequentially,
 * performing operations and potentially modifying the list during traversal.
 * Specific behaviors for each {@code CodeElement} type are defined in subclasses by
 * overriding the {@code visit} methods from {@code CodeElementVisitor}.
 *
 * This class simplifies the implementation of assembly-level transformations or optimizations
 * by managing the traversal logic and delegating the processing of individual elements
 * to the implemented visitor methods.
 *
 * Subclasses are required to provide specific logic for visiting {@code Instruction} and
 * {@code EncodedData} objects, as defined by the {@code CodeElementVisitor} interface.
 */
public abstract class AbstractAsmLevelLinearPass implements CodeElementVisitor<Boolean> {

    public boolean runPass(LinkedList<CodeElement> code) {
        boolean changed = false;
        ExtendedListIterator<CodeElement> iterator = new ExtendedListIterator<>(code);

        while (iterator.hasNext()) {
            CodeElement elem = iterator.next();

            if (elem.accept(this, iterator)) {
                changed = true;
            }
        }
        return changed;
    }
}
