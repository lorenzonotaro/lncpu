package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.AddressOf;

public class MultiLinearIROptimizerPass extends LinearIROptimizerPass {

    private final LinearIROptimizerPass[] passes;

    private MultiLinearIROptimizerPass() {
        this.passes = new LinearIROptimizerPass[0];
    }

    public MultiLinearIROptimizerPass(LinearIROptimizerPass... passes) {
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
