package com.lnc.cc.optimization.ir;

import com.lnc.LNC;
import com.lnc.cc.codegen.LivenessInfo;
import com.lnc.cc.ir.IRUnit;

import java.util.List;

/**
 * The StageOneIROptimizer class performs the first stage of optimization on an
 * Intermediate Representation (IR) unit by applying a series of predefined optimization
 * passes in a loop until a fixed point is reached or a maximum iteration limit is exceeded.
 *
 * Optimization passes include:
 * - Constant Folding: Simplifies expressions by replacing constants with their computed values.
 * - Copy Propagation and Move Forwarding: Reduces redundant copies and moves in the IR.
 * - Local Dead Code Elimination: Eliminates instructions that do not contribute to the program's outcome.
 * - Trivial Goto Elimination: Removes unnecessary goto statements.
 * - Dead Move Elimination: Removes redundant move instructions for unused values.
 *
 * The optimizer tracks whether any changes are made during each pass. If any pass modifies the IR,
 * liveness information is recomputed, which is then used for subsequent passes in the current iteration.
 *
 * If the optimization reaches the user-defined iteration limit (`--first-pass-opt-max-iter` setting),
 * an exception is thrown to prevent infinite loops.
 */
public class StageOneIROptimizer {

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
