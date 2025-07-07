package com.lnc.cc.codegen;

import com.lnc.assembler.parser.CodeElement;

import java.util.LinkedList;

public abstract class AbstractAsmLevelLinearPass implements CodeElementVisitor<Boolean> {

    private AsmCursor cursor;

    public boolean runPass(LinkedList<CodeElement> code) {
        boolean changed = false;
        AsmCursor cur = new AsmCursor(code);

        this.cursor = cur;

        while (cur.hasNext()) {
            CodeElement elem = cur.next();

            if (elem.accept(this)) {
                changed = true;
            }
        }

        this.cursor = null;

        return changed;
    }

    public final AsmCursor getCursor() {
        if (cursor == null) {
            throw new IllegalStateException("visit() called before visit(AsmCursor)");
        }
        return cursor;
    }
}
