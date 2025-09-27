package com.lnc.cc.optimization.ir;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public class DeadMoveEliminationPass extends IRPass{

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
        // dead move elimination
        if(move.getDest() instanceof VirtualRegister vr && isDeadAfter(vr, move)){
            deleteAndContinue();
            markAsChanged();
        }
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
                if (nextMov.getDest().equals(left)) {
                    bin.setDest(left);        // tmp ← op left,right   →   left ← op left,right
                    nextMov.remove();
                    markAsChanged();
                } else if (bin.getOperator().isCommutative() &&
                        nextMov.getDest().equals(right)) {
                    // swap operands so that 'right' becomes the two-address target
                    bin.swapOperands();       // helper you may need to add
                    bin.setDest(right);
                    nextMov.remove();
                    markAsChanged();
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
    public Void visit(Unary unary) {
        if(
                !unary.getTarget().equals(unary.getOperand()) &&
                        unary.hasNext() &&
                        unary.getNext() instanceof Move nextMove &&
                        nextMove.getDest().equals(unary.getOperand()) &&
                        nextMove.getSource().equals(unary.getTarget()) &&
                        isDeadAfter((VirtualRegister) unary.getTarget(), nextMove)
        ){
            // unary and store back -> eliminate the move
            unary.setTarget(unary.getOperand());
            nextMove.remove();
            markAsChanged();
        }
        return null;
    }

}
