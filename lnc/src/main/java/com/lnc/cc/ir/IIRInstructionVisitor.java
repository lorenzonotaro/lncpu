package com.lnc.cc.ir;

public interface IIRInstructionVisitor<E> {
    E accept(Goto aGoto);

    E accept(CondJump jle);

    E accept(Load load);

    E accept(Move move);

    E accept(Store store);

    E accept(Ret sub);

    E accept(Bin bin);

    E accept(Call call);

    E accept(Unary unary);

    default E accept(Label label) {
        return null;
    }

}
