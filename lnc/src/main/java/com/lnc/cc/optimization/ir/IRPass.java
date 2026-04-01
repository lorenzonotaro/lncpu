package com.lnc.cc.optimization.ir;

import com.lnc.cc.codegen.LivenessInfo;
import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.VirtualRegister;

/**
 * An abstract base class that serves as the foundation for implementing various
 * Intermediate Representation (IR) analysis and transformation passes.
 * Subclasses are expected to provide custom behavior by overriding visit methods
 * for specific IR instructions or blocks.
 *
 * This class ensures a consistent traversal order and provides a mechanism
 * for tracking changes to the IR during a pass.
 */
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
        return livenessInfo.isDeadAfter(vr, instr);
    }

    boolean isLiveAfter(VirtualRegister vr, IRInstruction instr) {
        return livenessInfo.isLiveAfter(vr, instr);
    }
}
