package com.lnc.cc.ir;

public interface IIRInstructionVisitor<E> {
    E visit(Goto aGoto);

    E visit(CondJump condJump);

    E visit(Load load);

    E visit(Move move);

    E visit(Store store);

    E visit(Ret ret);

    E visit(Bin bin);

    E visit(Call call);

    E visit(Unary unary);

    E visit(Push push);

    E accept(LoadParam loadParam);
}
