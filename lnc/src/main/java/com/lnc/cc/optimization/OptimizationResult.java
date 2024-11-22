package com.lnc.cc.optimization;

import com.lnc.cc.ir.IR;

import java.util.List;

public class OptimizationResult extends IR {

    private final List<LinearIRUnit> linearizedIRUnits;

    protected OptimizationResult(IR ir, List<LinearIRUnit> linearizedIRUnits) {
        super(ir);
        this.linearizedIRUnits = linearizedIRUnits;
    }

    public List<LinearIRUnit> getLinearizedIRUnits() {
        return linearizedIRUnits;
    }
}
