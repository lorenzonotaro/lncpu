package com.lnc.cc.codegen;

import com.lnc.assembler.parser.CodeElement;

import java.util.LinkedList;
import java.util.List;

public class AsmLevelOptimizer {
    private final List<AbstractAsmLevelLinearPass> passes = List.of(
            new RedundantGotoEliminationPass(),
            new CommuteAndEliminateMovePass()
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

    public void addRegisterPreservation(CompilerOutput output) {

        if(output.unit() == null) {
            throw new IllegalStateException("Cannot add register preservation pass without an IR unit.");
        }

        RegPreservationPass pass = new RegPreservationPass(output.unit());
        pass.runPass(output.code());
    }
}
