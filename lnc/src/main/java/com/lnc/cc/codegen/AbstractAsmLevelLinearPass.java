package com.lnc.cc.codegen;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.common.ExtendedListIterator;

import java.util.LinkedList;

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
