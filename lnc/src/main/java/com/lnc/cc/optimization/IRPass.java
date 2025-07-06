package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;

import java.util.Iterator;

public abstract class IRPass extends GraphicalIRVisitor{

    private boolean changed = false;

    @Override
    public final void visit(IRUnit unit){
        this.changed = false;
        super.visit(unit);
    }

    public final boolean isChanged() {
        return changed;
    }

    protected final void markAsChanged() {
        this.changed = true;
    }
}
