package com.lnc.cc.optimization;

import com.lnc.cc.ir.GraphicalIRVisitor;
import com.lnc.cc.ir.IRUnit;

public class FirstPassIROptimizer {

    private boolean changed = true;

    public void run(IRUnit unit){

        while(changed){
            setChanged(false);
            this.computeSuccessorsAndPredecessors(unit);
        }
    }

    private void computeSuccessorsAndPredecessors(IRUnit unit) {
        for (var block : unit) {
            block.computeSuccessorsAndPredecessors();
        }
    }

    protected boolean isChanged() {
        return changed;
    }

    protected void setChanged(boolean changed) {
        this.changed = changed;
    }
}
