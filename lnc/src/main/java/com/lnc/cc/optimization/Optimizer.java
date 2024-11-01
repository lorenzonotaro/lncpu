package com.lnc.cc.optimization;

import com.lnc.cc.ir.IRUnit;

import java.util.List;

public class Optimizer {

    private final List<IRUnit> units;

    public Optimizer(List<IRUnit> units) {
        this.units = units;
    }

    public void optimize() {
        for (IRUnit unit : units) {
            LinearIRUnit linearIRUnit = new LinearIRUnit(unit);
            linearIRUnit.linearize();

            linearIRUnit.visit();
        }
    }


}
