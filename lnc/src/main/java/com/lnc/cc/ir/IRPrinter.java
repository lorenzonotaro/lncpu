package com.lnc.cc.ir;

import java.util.List;

public class IRPrinter extends BranchingIRVisitor{
    
    private StringBuilder sb = new StringBuilder();
    
    @Override
    public Void accept(Goto aGoto) {
        sb.append("    ").append(aGoto.toString()).append("\n");
        return null;
    }

    @Override
    public Void accept(CondJump condJump) {
        sb.append("    ").append(condJump.toString()).append("\n");
        return null;
    }

    @Override
    public Void accept(Load load) {
        sb.append("    ").append(load.toString()).append("\n");
        return null;
    }

    @Override
    public Void accept(Move move) {
        sb.append("    ").append(move.toString()).append("\n");
        return null;
    }

    @Override
    public Void accept(Store store) {
        sb.append("    ").append(store.toString()).append("\n");
        return null;
    }

    @Override
    public Void accept(Ret sub) {
        sb.append("    ").append(sub.toString()).append("\n");
        return null;
    }

    @Override
    public Void accept(Bin bin) {
        sb.append("    ").append(bin.toString()).append("\n");
        return null;
    }

    @Override
    public Void accept(Call call) {
        sb.append("    ").append(call.toString()).append("\n");
        return null;
    }

    @Override
    public Void accept(Unary unary) {
        sb.append("    ").append(unary.toString()).append("\n");
        return null;
    }

    @Override
    public Void accept(Label label) {
        sb.append("\n").append(label.toString()).append(":\n");
        return super.accept(label);
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
    
    public String print(List<IRUnit> units) {
        for (IRUnit unit : units) {
            sb.append("==== Function: ").append(unit.getFunctionDeclaration().name.lexeme).append(" ====");
            visit(unit.getEntryBlock());
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
