package com.lnc.cc.codegen;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.IROperand;
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
    public Void visit(Load load) {
        return null;
    }

    @Override
    public Void visit(Move move) {
        // dead move elimination
        if(move.getDest() instanceof VirtualRegister vr && isDeadAfter(vr, move)){
            deleteAndContinue();
        }
        return null;
    }

    @Override
    public Void visit(Store store) {
        return null;
    }

    @Override
    public Void visit(Ret ret) {
        return null;
    }

    @Override
    public Void visit(Bin bin) {
        // Binary with dead-after temporary operand
        IRInstruction next = null;
        if(((next = bin.getNext()) != null && (next instanceof Move nextMov) && (nextMov.getDest().type == IROperand.Type.VIRTUAL_REGISTER))){

            VirtualRegister left  = (bin.getLeft()  instanceof VirtualRegister vrLeft)  ? vrLeft  : null;
            VirtualRegister right = (bin.getRight() instanceof VirtualRegister vrRight) ? vrRight : null;
            VirtualRegister tmp   = (VirtualRegister) bin.getDest();        // current dest

            // mov destReg ← tmp  ?
            if (tmp.equals(nextMov.getSource()) &&
                    !tmp.equals(nextMov.getDest())  &&     // destReg ≠ tmp
                    isDeadAfter(tmp, nextMov)) {

                // prefer collapsing into the register the op already uses
                if (left != null && nextMov.getDest().equals(left)) {
                    bin.setDest(left);        // tmp ← op left,right   →   left ← op left,right
                    nextMov.remove();
                } else if (bin.getOperator().isCommutative() &&
                        right != null && nextMov.getDest().equals(right)) {
                    // swap operands so that 'right' becomes the two-address target
                    bin.swapOperands();       // helper you may need to add
                    bin.setDest(right);
                    nextMov.remove();
                }
            }
        }

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
    public Void accept(LoadParam loadParam) {
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
