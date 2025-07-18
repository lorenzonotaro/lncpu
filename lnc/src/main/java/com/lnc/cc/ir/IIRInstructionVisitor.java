package com.lnc.cc.ir;

public interface IIRInstructionVisitor<E> {
    E visit(Goto aGoto);

    E visit(CondJump condJump);

    E visit(Move move);

    E visit(Ret ret);

    E visit(Bin bin);

    E visit(Call call);

    E visit(Push push);

    E accept(LoadParam loadParam);

    E visit(Unary unary);

}
