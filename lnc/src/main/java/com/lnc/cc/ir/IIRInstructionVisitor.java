package com.lnc.cc.ir;

public interface IIRInstructionVisitor<E> {
    E visit(Goto aGoto);

    E visit(CondJump jle);

    E visit(Load load);

    E visit(Move move);

    E visit(Store store);

    E visit(Ret sub);

    E visit(Bin bin);

    E visit(Call call);

    E visit(Unary unary);

    default E visit(Label label) {
        return null;
    }

}
