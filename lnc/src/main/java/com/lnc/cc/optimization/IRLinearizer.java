package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;

public class IRLinearizer extends BranchingIRVisitor {
    @Override
    public Void accept(Goto aGoto) {
        return null;
    }

    @Override
    public Void accept(Dec dec) {
        return null;
    }

    @Override
    public Void accept(Inc inc) {
        return null;
    }

    @Override
    public Void accept(Load load) {
        return null;
    }

    @Override
    public Void accept(Move move) {
        return null;
    }

    @Override
    public Void accept(Store store) {
        return null;
    }

    @Override
    public Void accept(Ret sub) {
        return null;
    }

    @Override
    public Void accept(Neg neg) {
        return null;
    }

    @Override
    public Void accept(Not not) {
        return null;
    }

    @Override
    public Void accept(Bin bin) {
        return null;
    }

    @Override
    public Void accept(Call call) {
        return null;
    }
}
