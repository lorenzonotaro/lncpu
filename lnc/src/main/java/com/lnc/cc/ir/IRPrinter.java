package com.lnc.cc.ir;

import java.util.List;

public class IRPrinter extends GraphicalIRVisitor {
    
    private final StringBuilder sb = new StringBuilder();
    
    @Override
    public Void visit(Goto aGoto) {
        sb.append("    ").append(aGoto.toString()).append("\n");
        return null;
    }

    @Override
    public Void visit(CondJump condJump) {
        sb.append("    ").append(condJump.toString()).append("\n");
        return null;
    }

    @Override
    public Void visit(Load load) {
        sb.append("    ").append(load.toString()).append("\n");
        return null;
    }

    @Override
    public Void visit(Move move) {
        sb.append("    ").append(move.toString()).append("\n");
        return null;
    }

    @Override
    public Void visit(Store store) {
        sb.append("    ").append(store.toString()).append("\n");
        return null;
    }

    @Override
    public Void visit(Ret sub) {
        sb.append("    ").append(sub.toString()).append("\n");
        return null;
    }

    @Override
    public Void visit(Bin bin) {
        sb.append("    ").append(bin.toString()).append("\n");
        return null;
    }

    @Override
    public Void visit(Call call) {
        sb.append("    ").append(call.toString()).append("\n");
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        sb.append("    ").append(unary.toString()).append("\n");
        return null;
    }

    @Override
    public Void visit(Label label) {
        sb.append("\n").append(label.toString()).append(":\n");
        return super.visit(label);
    }

    @Override
    protected boolean visit(IRBlock block) {
        if (block == null || isVisited(block)) {
            return false;
        }

        sb.append("\n_l").append(block.getId()).append(":\n");

        super.visit(block);
        return true;
    }

    @Override
    public void visit(IRUnit unit) {
        sb.append("==== Function: ").append(unit.getFunctionDeclaration().name.lexeme).append(" ====");
        super.visit(unit);
        sb.append("\n");
    }
}
