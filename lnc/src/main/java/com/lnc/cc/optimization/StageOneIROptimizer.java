package com.lnc.cc.optimization;

import com.lnc.LNC;
import com.lnc.cc.codegen.GraphColoringRegisterAllocator;
import com.lnc.cc.codegen.LivenessInfo;
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
            new CopyPropagationAndMoveForwardingPass(),
            new LocalDeadCodeEliminationPass(),
            new TrivialGotoEliminationPass(),
            new DeadMoveEliminationPass()
    );

    public void run(IRUnit unit){
        boolean changed;
        int maxIter = LNC.settings.get("--first-pass-opt-max-iter", Double.class).intValue();
        do{
            if(maxIter-- <= 0) {
                throw new RuntimeException("Exceeded maximum iterations for first pass optimization.");
            }
            changed = false;
            LivenessInfo livenessInfo = LivenessInfo.computeBlockLiveness(unit);
            for (var pass : PASSES) {
                pass.setLivenessInfo(livenessInfo);
                pass.visit(unit);
                boolean passChanged = pass.isChanged();
                changed |= passChanged;

                if(passChanged) {
                    livenessInfo = LivenessInfo.computeBlockLiveness(unit);
                }
            }
        }while(changed);
    }


}
