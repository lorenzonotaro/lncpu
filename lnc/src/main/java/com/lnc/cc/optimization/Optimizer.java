package com.lnc.cc.optimization;

import com.lnc.LNC;
import com.lnc.cc.ir.IR;
import com.lnc.cc.ir.IRUnit;

import java.util.ArrayList;
import java.util.List;

public class Optimizer extends MultiIROptimizerPass {

    private final IR ir;

    public Optimizer(IR ir) {
        super(new ControlFlowRedundancyOptimizerPass());
        this.ir = ir;
    }

    public OptimizationResult optimize() {

        List<LinearIRUnit> linearIRUnits = new ArrayList<>();

        IRLinearizer linearizer = new IRLinearizer();

        var doOptimize = !LNC.settings.get("--no-optimization", Boolean.class);

        for (IRUnit unit : ir.units()) {
            LinearIRUnit linearIRUnit = linearizer.linearize(unit);

            if(doOptimize){
                var modified = false;
                do {
                    modified = apply(linearIRUnit);
                } while (modified);
            }

            linearIRUnits.add(linearIRUnit);
        }

        return new OptimizationResult(ir, linearIRUnits);
    }


}
