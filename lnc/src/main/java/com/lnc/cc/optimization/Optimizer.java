package com.lnc.cc.optimization;

import com.lnc.cc.ir.IR;
import com.lnc.cc.ir.IRUnit;

public class Optimizer {

    private final IR ir;

    public Optimizer(IR ir) {
        this.ir = ir;
    }

    public void optimize() {
        for (IRUnit unit : ir.units()) {
            LinearIRUnit linearIRUnit = new LinearIRUnit(unit);
            linearIRUnit.linearize();

            linearIRUnit.visit();
        }
    }


}
