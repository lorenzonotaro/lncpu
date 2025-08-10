package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;

public class ConstantFoldingPass extends IRPass {
    private final ConstantPropagationEvaluator evaluator;

    public ConstantFoldingPass() {
        this.evaluator = new ConstantPropagationEvaluator();
    }

    @Override
    public Void visit(Goto aGoto) {
        return null;
    }

    @Override
    public Void visit(CondJump condJump) {

        PropValue condValue = propagateCondition(condJump);

        if (condValue.isConstant()) {
            if (condValue.valueOr(0) == 0) {
                // If condition is false, redirect to the false target
                super.replaceAndContinue(new Goto(condJump.getFalseTarget()));
            } else {
                // If condition is true, redirect to the true target
                super.replaceAndContinue(new Goto(condJump.getTarget()));
            }
            markAsChanged();
        }

        return null;
    }

    private PropValue propagateCondition(CondJump condJump) {
        PropValue left = condJump.getLeft().accept(evaluator);
        PropValue right = condJump.getRight().accept(evaluator);

        if(left.isConstant() && right.isConstant()) {
            return switch (condJump.getCond()) {
                case EQ -> PropValue.constant(left.valueOr(0) == right.valueOr(0) ? 1 : 0);
                case NE -> PropValue.constant(left.valueOr(0) != right.valueOr(0) ? 1 : 0);
                case LT -> PropValue.constant(left.valueOr(0) < right.valueOr(0) ? 1 : 0);
                case LE -> PropValue.constant(left.valueOr(0) <= right.valueOr(0) ? 1 : 0);
                case GT -> PropValue.constant(left.valueOr(0) > right.valueOr(0) ? 1 : 0);
                case GE -> PropValue.constant(left.valueOr(0) >= right.valueOr(0) ? 1 : 0);
            };
        }

        return PropValue.unknown();
    }

    @Override
    public Void visit(Move move) {
        return null;
    }

    @Override
    public Void visit(Ret ret) {
        return null;
    }

    @Override
    public Void visit(Bin bin) {
        // TODO
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
        // TODO
        return null;
    }


}
