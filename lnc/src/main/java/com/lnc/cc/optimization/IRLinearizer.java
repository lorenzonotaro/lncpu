package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.*;

public class IRLinearizer extends BranchingIRVisitor {


    private LinearIRUnit currentUnit;

    @Override
    public Void accept(Jle jle) {
        append(jle);

        visit(jle.getNonTakenBranch());

        visit(jle.getTarget());

        return null;
    }

    @Override
    public Void accept(Jeq je) {
        append(je);

        visit(je.getNonTakenBranch());

        visit(je.getTarget());

        return null;
    }

    @Override
    public Void accept(Jlt jlt) {

        append(jlt);

        visit(jlt.getNonTakenBranch());

        visit(jlt.getTarget());

        return null;
    }

    @Override
    protected boolean visit(IRBlock block) {

        if(block == null || isVisited(block)){
            return false;
        }

        append(new Label(block));

        return super.visit(block);
    }

    @Override
    public Void accept(Goto aGoto) {
        append(aGoto);
        return null;
    }

    @Override
    public Void accept(Dec dec) {
        append(dec);
        return null;
    }

    @Override
    public Void accept(Inc inc) {
        append(inc);
        return null;
    }

    @Override
    public Void accept(Load load) {
        append(load);
        return null;
    }

    @Override
    public Void accept(Move move) {
        append(move);
        return null;
    }

    @Override
    public Void accept(Store store) {
        append(store);
        return null;
    }

    @Override
    public Void accept(Ret sub) {
        append(sub);
        return null;
    }

    @Override
    public Void accept(Neg neg) {
        append(neg);
        return null;
    }

    @Override
    public Void accept(Not not) {
        append(not);
        return null;
    }

    @Override
    public Void accept(Bin bin) {
        append(bin);
        return null;
    }

    @Override
    public Void accept(Call call) {
        append(call);
        return null;
    }


    @Override
    protected void append(IRInstruction instruction) {
        currentUnit.append(instruction);
    }


    public LinearIRUnit linearize(IRUnit unit) {
        this.currentUnit = new LinearIRUnit(unit);

        visit(unit.getEntryBlock());

        return currentUnit;
    }

    @Override
    public Void accept(ImmediateOperand immediateOperand) {
        return null;
    }

    @Override
    public Void accept(VirtualRegister vr) {
        return null;
    }

    @Override
    public Void accept(RegisterDereference rd) {
        return null;
    }

    @Override
    public Void accept(Location location) {
        return null;
    }

    @Override
    public Void accept(AddressOf addressOf) {
        return null;
    }
}
