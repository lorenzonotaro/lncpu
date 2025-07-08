package com.lnc.cc.codegen;

import java.util.List;

public class AsmLevelOptimizer {
    private final List<AbstractAsmLevelLinearPass> passes = List.of(
            //new RedundantGotoEliminationPass(),
            //new CommuteAndEliminateMovePass()
    );

    public void optimize(CompilerOutput output) {
        boolean changed;
        do {
            changed = false;
            for (AbstractAsmLevelLinearPass pass : passes) {
                if (pass.runPass(output.code())) {
                    changed = true;
                }
            }
        } while (changed);
    }

    public void stackFramePreservation(CompilerOutput output) {

        if(output.unit() == null) {
            throw new IllegalStateException("Cannot add register preservation pass without an IR unit.");
        }

        StackFramePreservationPass pass = new StackFramePreservationPass(output.unit());
        pass.runPass(output.code());
    }
}
