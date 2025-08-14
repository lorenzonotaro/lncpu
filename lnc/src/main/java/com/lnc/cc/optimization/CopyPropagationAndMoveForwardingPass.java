package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

import java.util.*;

/**
 * Intra-block copy propagation and move forwarding.
 * - Tracks latest representative for each VirtualRegister value when encountering moves (dest <- src).
 * - Rewrites subsequent reads to use the latest representative (prefer the most recent copy) to shorten older live ranges.
 * - Conservatively kills mappings on clobbers (any non-move write).
 * - Removes moves that become trivial (dest == src) after propagation.
 */
public class CopyPropagationAndMoveForwardingPass extends IRPass {

    // Map from a VR to the VR we prefer to read from for its current value (latest representative).
    private final Map<VirtualRegister, VirtualRegister> latest = new HashMap<>();

    @Override
    protected void visit(IRBlock block) {
        latest.clear();
        super.visit(block);
    }

    private VirtualRegister find(VirtualRegister v) {
        // Find with simple path compression
        VirtualRegister cur = v;
        VirtualRegister next = latest.get(cur);
        if (next == null) {
            latest.put(cur, cur);
            return cur;
        }
        while (next != cur) {
            VirtualRegister n2 = latest.get(next);
            if (n2 == null) {
                latest.put(next, next);
                n2 = next;
            }
            cur = next;
            next = n2;
        }
        // Path compression
        VirtualRegister rep = cur;
        cur = v;
        while ((next = latest.get(cur)) != null && next != cur) {
            latest.put(cur, rep);
            cur = next;
        }
        return rep;
    }

    // Redirect all VRs that currently resolve to 'oldRep' to now resolve to 'newRep'
    private void redirectAllTo(VirtualRegister oldRep, VirtualRegister newRep) {
        // Snapshot keys to avoid concurrent modification
        List<VirtualRegister> keys = new ArrayList<>(latest.keySet());
        for (VirtualRegister k : keys) {
            if (find(k).equals(oldRep)) {
                latest.put(k, newRep);
            }
        }
        latest.put(oldRep, newRep);
    }

    // Kill any equivalence that resolves to 'w' (because 'w' is clobbered by a non-move write)
    private void kill(VirtualRegister w) {
        List<VirtualRegister> keys = new ArrayList<>(latest.keySet());
        for (VirtualRegister k : keys) {
            if (find(k).equals(w)) {
                latest.put(k, k);
            }
        }
        latest.put(w, w);
    }

    private void killAll(Collection<VirtualRegister> writes) {
        for (VirtualRegister w : writes) {
            kill(w);
        }
    }

    // Generic read replacement for an instruction; conservative: do not rewrite if the VR is also written by the same instruction
    private void rewriteReads(IRInstruction instr) {
        Set<VirtualRegister> written = new HashSet<>(instr.getWrites());
        for (IROperand rop : instr.getReadOperands()) {
            if (rop instanceof VirtualRegister vr) {
                if (written.contains(vr)) continue; // conservative
                VirtualRegister rep = find(vr);
                if (!rep.equals(vr)) {
                    instr.replaceOperand(vr, rep);
                    markAsChanged();
                }
            }
        }
    }

    @Override
    public Void visit(Goto aGoto) {
        // No operands to rewrite; no kills needed
        return null;
    }

    @Override
    public Void visit(CondJump condJump) {
        rewriteReads(condJump);
        // CondJump writes nothing => no kills
        return null;
    }

    @Override
    public Void visit(Move move) {
        // First, try to rewrite the read side (source) to the latest representative
        IROperand src = move.getSource();
        IROperand dst = move.getDest();

        if (src instanceof VirtualRegister srcVr) {
            VirtualRegister rep = find(srcVr);
            // Avoid creating move x <- x due to immediate replacement; we'll handle deletion below
            if (!rep.equals(srcVr)) {
                move.replaceOperand(srcVr, rep);
                src = rep;
                markAsChanged();
            }
        } else {
            // Non-VR source: nothing to rewrite in terms of alias tracking
        }

        // If move became trivial (dest == src), remove it
        if (src instanceof VirtualRegister s && dst instanceof VirtualRegister d && s.equals(d)) {
            deleteAndContinue();
            markAsChanged();
            return null;
        }

        // Update equivalence classes:
        // The move defines 'dest'. First, kill any previous info about 'dest' since it's being overwritten now.
        if (dst instanceof VirtualRegister dvr) {
            kill(dvr);

            // If source is a VR, now prefer the newest copy 'dest' as the representative for that value.
            if (src instanceof VirtualRegister svr) {
                VirtualRegister sRep = find(svr);
                // Direct everyone equal to sRep to now prefer dvr
                redirectAllTo(sRep, dvr);
                // Ensure dvr resolves to itself (newest)
                latest.put(dvr, dvr);
            } else {
                // Non-VR source: no alias formed; just note dvr resolves to itself for now
                latest.put(dvr, dvr);
            }
        }

        return null;
    }

    @Override
    public Void visit(Ret ret) {
        rewriteReads(ret);
        // No writes; nothing to kill
        return null;
    }

    @Override
    public Void visit(Bin bin) {
        // Rewrite reads
        rewriteReads(bin);
        // Kill clobbered destination(s)
        killAll(bin.getWrites());
        return null;
    }

    @Override
    public Void visit(Call call) {
        // Rewrite read operands (arguments)
        rewriteReads(call);
        // Kill written result register(s)
        killAll(call.getWrites());
        return null;
    }

    @Override
    public Void visit(Push push) {
        rewriteReads(push);
        // Push writes nothing to VRs
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        rewriteReads(unary);
        killAll(unary.getWrites());
        return null;
    }
}
