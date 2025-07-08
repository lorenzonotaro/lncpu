package com.lnc.cc.optimization;

import com.lnc.LNC;
import com.lnc.cc.ir.IRUnit;

import java.util.List;

public class StageOneIROptimizer {

    /*
    * private static final List<IRPass> PASSES = List.of(
    new ConstantFoldingPass(),
    new CopyPropagationPass(),
    new LocalDeadCodeEliminationPass(),
    new TrivialGotoEliminationPass(),
    new UnreachableBlockEliminationPass(),
    new BlockMergingPass(),
    new BranchSimplificationPass()
);
* */

    private static final List<IRPass> PASSES = List.of(
            new ConstantFoldingPass(),
            new LocalDeadCodeEliminationPass(),
            new TrivialGotoEliminationPass()
    );

    public void run(IRUnit unit){
        boolean changed;
        int maxIter = LNC.settings.get("--first-pass-opt-max-iter", Double.class).intValue();
        do{
            if(maxIter-- <= 0) {
                throw new RuntimeException("Exceeded maximum iterations for first pass optimization.");
            }
            changed = false;
            for (var pass : PASSES) {
                pass.visit(unit);
                boolean passChanged = pass.isChanged();

                changed |= passChanged;
            }
        }while(changed);
    }

}
