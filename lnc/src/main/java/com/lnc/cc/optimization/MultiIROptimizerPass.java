package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.AddressOf;

public class MultiIROptimizerPass extends LinearIROptimizerPass {

    private final LinearIROptimizerPass[] passes;

    private MultiIROptimizerPass() {
        this.passes = new LinearIROptimizerPass[0];
    }

    public MultiIROptimizerPass(LinearIROptimizerPass... passes) {
        this.passes = passes;
    }

    @Override
    public boolean visit(IRInstruction instruction) {
        boolean result = false;

        for (LinearIROptimizerPass pass : passes) {
            result |= pass.visit(instruction);
        }

        return result;
    }

    @Override
    protected void setCurrentUnit(LinearIRUnit unit) {
        super.setCurrentUnit(unit);
        for (LinearIROptimizerPass pass : passes) {
            pass.setCurrentUnit(unit);
        }
    }

    @Override
    public Boolean accept(AddressOf addressOf) {
        return null;
    }
}
