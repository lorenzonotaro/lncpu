package com.lnc.cc.types;

import com.lnc.cc.ast.VariableDeclaration;

public class StructFieldEntry {
    private int offset;
    private VariableDeclaration field;

    public StructFieldEntry(int offset, VariableDeclaration field) {
        this.offset = offset;
        this.field = field;
    }

    public VariableDeclaration getField() {
        return field;
    }

    public int getOffset() {
        return offset;
    }
}
