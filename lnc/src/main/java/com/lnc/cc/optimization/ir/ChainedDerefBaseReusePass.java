package com.lnc.cc.optimization.ir;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.DerefLocation;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

/**
 * Reuses the dereference base register for chained far-pointer deref patterns to
 * shorten live ranges before register allocation.
 *
 * Pattern:
 *   move tmp <- *base
 *   move next <- tmp
 *   ... use next ...
 *
 * Rewritten (when safe):
 *   move base <- *base
 *   ... use base ...
 */
public class ChainedDerefBaseReusePass extends IRPass {
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
        if (!(move.getSource() instanceof DerefLocation deref)) {
            return null;
        }
        if (!(deref.getTarget() instanceof VirtualRegister baseVr)) {
            return null;
        }
        if (!(move.getDest() instanceof VirtualRegister loadedVr)) {
            return null;
        }

        if (baseVr.getTypeSpecifier().allocSize() != 2 || loadedVr.getTypeSpecifier().allocSize() != 2) {
            return null;
        }

        if (!isDeadAfter(baseVr, move)) {
            return null;
        }

        IRInstruction next = move.getNext();
        if (!(next instanceof Move copyMove)) {
            return null;
        }
        if (!(copyMove.getSource() instanceof VirtualRegister copySrc) || copySrc != loadedVr) {
            return null;
        }
        if (!(copyMove.getDest() instanceof VirtualRegister innerVr)) {
            return null;
        }
        if (innerVr.getTypeSpecifier().allocSize() != 2) {
            return null;
        }

        if (!isDeadAfter(loadedVr, copyMove)) {
            return null;
        }

        move.setDest(baseVr);
        replaceUsesUntilRedef(copyMove.getNext(), innerVr, baseVr);

        if (isDeadAfter(innerVr, copyMove)) {
            copyMove.remove();
        }

        markAsChanged();
        return null;
    }

    private static void replaceUsesUntilRedef(IRInstruction start,
                                              VirtualRegister oldVr,
                                              VirtualRegister newVr) {
        for (IRInstruction cursor = start; cursor != null; cursor = cursor.getNext()) {
            if (cursor.getWrites().contains(oldVr)) {
                break;
            }
            cursor.replaceOperand(oldVr, newVr);
        }
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
    public Void visit(Pop pop) {
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        return null;
    }
}

