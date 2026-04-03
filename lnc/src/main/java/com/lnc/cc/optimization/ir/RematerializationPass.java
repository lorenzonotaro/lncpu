package com.lnc.cc.optimization.ir;

import com.lnc.cc.codegen.LivenessInfo;
import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.ImmediateOperand;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

import java.util.List;

/**
 * Replaces eligible virtual-register reads with their defining immediate value.
 *
 * Candidate form:
 * - exactly one write to VR in the whole unit
 * - defining instruction is move immediate -> vr
 * - all reads are direct instruction operands (no nested location reads)
 * - each read site is safe for immediate substitution
 */
public class RematerializationPass extends IRPass {

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
        if (!(move.getDest() instanceof VirtualRegister vr)) {
            return null;
        }

        IROperand source = move.getSource();
        if (!(source instanceof ImmediateOperand immediate)) {
            return null;
        }

        LivenessInfo.DefUseStats stats = LivenessInfo.computeDefUseStats(getUnit(), vr);
        if (!stats.hasSingleWrite() || !move.equals(stats.singleWriter())) {
            return null;
        }

        if (!stats.allReadsAreDirect()) {
            return null;
        }

        List<IRInstruction> users = LivenessInfo.directReadUsers(getUnit(), vr);
        for (IRInstruction user : users) {
            if (!LivenessInfo.canReplaceDirectReadWithImmediate(user, vr)) {
                return null;
            }
        }

        for (IRInstruction user : users) {
            user.replaceOperand(vr, immediate);
        }

        deleteAndContinue();
        markAsChanged();
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
}

