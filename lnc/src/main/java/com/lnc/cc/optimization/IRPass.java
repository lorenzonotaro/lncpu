package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;

import java.util.Iterator;

public abstract class IRPass<I> implements IIRInstructionVisitor<I>{

    public boolean run(IRUnit unit){
        for (Iterator<IRBlock> iterator = unit.iterator(); iterator.hasNext(); ) {
            IRBlock block = iterator.next();
            iterator.ad
            visit(block);
        }
    }

}
