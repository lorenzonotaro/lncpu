package com.lnc.cc.codegen;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.VirtualRegister;

public class PostRAOptimizer extends GraphicalIRVisitor {

    private final IRUnit unit;
    private final GraphColoringRegisterAllocator.AllocationInfo allocationInfo;

    public PostRAOptimizer(IRUnit unit, GraphColoringRegisterAllocator.AllocationInfo allocationInfo) {
        super(TraversalOrder.REVERSE_POST_ORDER_ONLY);
        this.unit = unit;
        this.allocationInfo = allocationInfo;
    }

    public static void run(IRUnit unit, GraphColoringRegisterAllocator.AllocationInfo allocationInfo) {
        PostRAOptimizer optimizer = new PostRAOptimizer(unit, allocationInfo);
        optimizer.visit(unit);
    }

    @Override
    public Void visit(Goto aGoto) {
        return null;
    }

    @Override
    public Void visit(CondJump condJump) {
        return null;
    }

    @Override
    public Void visit(Move move) {
        return null;
    }

    @Override
    public Void visit(Ret ret) {
        return null;
    }

    @Override
    public Void visit(Bin bin) {
        return null;
    }

    @Override
    public Void visit(Call call) {
        return null;
    }

    @Override
    public Void visit(Push push) {
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        return null;
    }

    boolean isDeadAfter(VirtualRegister vr, IRInstruction instr) {
        // backward walk inside the current basic block
        for (IRInstruction p = instr.getNext(); p != null; p = p.getNext()) {
            if (p.getReads().contains(vr))   return false;    // value is read later
            if (p.getWrites().contains(vr))   return true;     // overwritten => previous value dead
        }
        // at block end: consult liveOut[block] bit-set
        return !this.allocationInfo.livenessInfo().liveOut().get(instr.getParentBlock()).contains(vr);
    }
}
