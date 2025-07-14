package com.lnc.cc.optimization;

import com.lnc.cc.codegen.LivenessInfo;
import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.VirtualRegister;

public abstract class IRPass extends GraphicalIRVisitor{

    private boolean changed = false;
    private LivenessInfo livenessInfo;

    public IRPass(){
        super(TraversalOrder.REVERSE_POST_ORDER_ONLY);
    }

    @Override
    public final void visit(IRUnit unit){
        this.changed = false;
        super.visit(unit);
    }

    public final boolean isChanged() {
        return changed;
    }

    protected final void markAsChanged() {
        this.changed = true;
    }

    public void setLivenessInfo(LivenessInfo livenessInfo) {
        this.livenessInfo = livenessInfo;
    }

    boolean isDeadAfter(VirtualRegister vr, IRInstruction instr) {
        // backward walk inside the current basic block
        for (IRInstruction p = instr.getNext(); p != null; p = p.getNext()) {
            if (p.getReads().contains(vr))   return false;    // value is read later
            if (p.getWrites().contains(vr))   return true;     // overwritten => previous value dead
        }
        // at block end: consult liveOut[block] bit-set
        return !this.livenessInfo.liveOut().get(instr.getParentBlock()).contains(vr);
    }
}
