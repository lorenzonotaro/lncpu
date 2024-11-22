package com.lnc.cc.optimization;

import com.lnc.cc.ir.Goto;
import com.lnc.cc.ir.ILabelReferenceHolder;
import com.lnc.cc.ir.Label;

public class ControlFlowRedundancyOptimizerPass extends MultiIROptimizerPass {
    public ControlFlowRedundancyOptimizerPass() {
        super(new UnusedLabelPass(), new RedundantGotoPass(), new RedundantLabelPass());
    }

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
}
