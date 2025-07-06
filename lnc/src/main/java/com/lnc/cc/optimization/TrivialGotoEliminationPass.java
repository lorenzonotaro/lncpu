package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;

public class TrivialGotoEliminationPass extends IRPass{
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
        return null;
    }

    @Override
    public Void visit(Call call) {
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        return null;
    }

    @Override
    public Void visit(Push push) {
        return null;
    }

    @Override
    protected void visit(IRBlock block) {
        var first = block.getFirst();
        var last = block.getLast();
        // If the block contains only a single Goto instruction, replace the block with its target
        if (first == last && first instanceof Goto aGoto){
            block.replaceWith(aGoto.getTarget());
            markAsChanged();
        }else{
            super.visit(block);
        }
    }
}
