package com.lnc.cc.optimization;

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
        do{
            changed = false;
            for (var pass : PASSES) {
                pass.visit(unit);
                boolean passChanged = pass.isChanged();

                if(passChanged){
                    System.out.println("Pass " + pass.getClass().getSimpleName() + " changed the IR.");
                }

                changed |= passChanged;
            }
        }while(changed);
    }

}
