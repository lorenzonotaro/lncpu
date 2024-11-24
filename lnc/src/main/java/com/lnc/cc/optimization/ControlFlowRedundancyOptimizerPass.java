package com.lnc.cc.optimization;

import com.lnc.cc.ir.Goto;
import com.lnc.cc.ir.ILabelReferenceHolder;
import com.lnc.cc.ir.IRInstruction;
import com.lnc.cc.ir.Label;

public class ControlFlowRedundancyOptimizerPass extends MultiIROptimizerPass {
    public ControlFlowRedundancyOptimizerPass() {
        super(new UnusedLabelPass(), new RedundantGotoPass(), new RedundantLabelPass(), new UnreachableCodePass());
    }

    /** Remove goto instructions that are immediately followed by a label that they target. */
    private static class RedundantGotoPass extends LinearIROptimizerPass {
        @Override
        public Boolean accept(Goto aGoto) {
            if(aGoto.hasNext() && (aGoto.getNext() instanceof Label label) && label.block.equals(aGoto.getTarget())){
                remove(aGoto);
                return true;
            }
            return false;
        }
    }

    /** Remove labels that are immediately followed by another label. */
    private static class RedundantLabelPass extends LinearIROptimizerPass {
        @Override
        public Boolean accept(Label label) {
            if(label.hasNext() && (label.getNext() instanceof Label nextLabel)){
                var references = label.block.getReferences();
                for(ILabelReferenceHolder ref : references){
                    ref.replaceReference(label.block, nextLabel.block);
                }
                remove(label);
                return true;
            }else if(label.hasNext() && (label.getNext() instanceof Goto nextGoto)){
                var references = label.block.getReferences();
                for(ILabelReferenceHolder ref : references){
                    ref.replaceReference(label.block, nextGoto.getTarget());
                }
                remove(label);
                return true;
            }
            return false;
        }
    }

    /** Remove labels that are not referenced by any instruction. */
    private static class UnusedLabelPass extends LinearIROptimizerPass {
        @Override
        public Boolean accept(Label label) {
            if(label.block.getReferences().isEmpty()){
                remove(label);
                return true;
            }
            return false;
        }
    }

    /** When encountering a goto instruction, remove all subsequent instruction until a label is called, as they will never execute. */
    private static class UnreachableCodePass extends LinearIROptimizerPass {
        @Override
        public Boolean accept(Goto aGoto) {
            if(aGoto.hasNext()){
                var removedAny = false;
                var current = aGoto.getNext();
                while(current != null && !(current instanceof Label)){
                    var next = current.getNext();
                    remove(current);
                    removedAny = true;
                    current = next;
                }
                return removedAny;
            }
            return false;
        }
    }
}
