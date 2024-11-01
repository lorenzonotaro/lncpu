package com.lnc.cc.ir;

public interface IIRVisitor<E> {
    E accept(Goto aGoto);

    E accept(Dec dec);

    E accept(Inc inc);

    E accept(Jge jge);

    E accept(Jle jle);

    E accept(Jne jne);

    E accept(Jeq je);

    E accept(Jlt jle);

    E accept(Jgt jgt);

    E accept(Load load);

    E accept(Move move);

    E accept(Store store);

    E accept(Ret sub);

    E accept(Neg neg);

    E accept(Not not);

    E accept(Bin bin);

    E accept(Call call);

    default E accept(Label label) {
        return null;
    }
}
