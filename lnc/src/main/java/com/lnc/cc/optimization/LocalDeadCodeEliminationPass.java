package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;

public class LocalDeadCodeEliminationPass extends IRPass{
    @Override
    public Void visit(Goto aGoto) {
        // Delete all subsequent instructions in the block
        if(aGoto.hasNext()){
            aGoto.setNext(null);
            aGoto.getParentBlock().setLast(aGoto);
            markAsChanged();
        }
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
        // Remove all subsequent instructions in the block
        if(ret.hasNext()){
            ret.setNext(null);
            ret.getParentBlock().setLast(ret);
            markAsChanged();
        }
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
    public Void accept(LoadParam loadParam) {
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        return null;
    }

}
